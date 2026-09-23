#!/usr/bin/env bash
#
# Build the debug APK and publish it so Obtainium can pick it up.
#
# The source code lives in the CODE_REPO; the APKs are published in a separate
# release repo (REL_REPO) with a rolling "latest" prerelease so a single stable
# URL always serves the newest build:
#
#   https://github.com/<owner>/<rel_repo>/releases/download/latest/medremind.apk
#
# Usage:
#   GH_TOKEN=<token> GRADLE=/path/to/gradle ./scripts/release.sh v0.2.0 "notes"
#
set -euo pipefail

OWNER="${REPO_OWNER:-wahidiqubal9-svg}"
CODE_REPO="${CODE_REPO_NAME:-MedRemind}"
REL_REPO="${REL_REPO_NAME:-MedRemind-Releases}"
GRADLE="${GRADLE:-gradle}"
TAG="${1:?usage: release.sh <tag, e.g. v0.2.0> [notes]}"
NOTES="${2:-Automatic build $TAG}"

: "${GH_TOKEN:?GH_TOKEN environment variable is required}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

APK="app/build/outputs/apk/debug/app-debug.apk"
API="https://api.github.com/repos/${OWNER}/${REL_REPO}"
AUTH=(-H "Authorization: Bearer ${GH_TOKEN}" -H "Accept: application/vnd.github+json")

echo "==> Building $TAG"
"$GRADLE" --no-daemon --console=plain assembleDebug

echo "==> Staging commit"
git add -A
if ! git diff --cached --quiet; then
  git -c user.name="${GIT_NAME:-$OWNER}" \
      -c user.email="${GIT_EMAIL:-$OWNER@users.noreply.github.com}" \
      commit -m "Release $TAG"
fi
git -c credential.helper= push "https://x-access-token:${GH_TOKEN}@github.com/${OWNER}/${CODE_REPO}.git" HEAD:main

upload_asset() { # $1=upload_url  $2=asset_name
  curl -sS -X POST "${AUTH[@]}" \
    -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary "@${APK}" \
    "$1?name=$2"
}

echo "==> Creating release $TAG in ${OWNER}/${REL_REPO}"
curl -sS -X POST "${AUTH[@]}" "${API}/releases" \
  -d "{\"tag_name\":\"${TAG}\",\"target_commitish\":\"main\",\"name\":\"${TAG}\",\"body\":\"${NOTES}\",\"draft\":false,\"prerelease\":false}" \
  > /tmp/release.json

UPLOAD_URL="$(grep -o '"upload_url": "[^"]*' /tmp/release.json | sed 's/"upload_url": "//' | sed 's/{.*//')"
if [ -z "$UPLOAD_URL" ]; then
  echo "Failed to create release:"; cat /tmp/release.json; exit 1
fi

ASSET_NAME="medremind-${TAG}-debug.apk"
echo "==> Uploading ${ASSET_NAME}"
upload_asset "${UPLOAD_URL}" "${ASSET_NAME}" | grep -o '"browser_download_url": "[^"]*' | sed 's/"browser_download_url": "//'

echo "==> Uploading medremind-latest.apk"
upload_asset "${UPLOAD_URL}" "medremind-latest.apk" | grep -o '"browser_download_url": "[^"]*' | sed 's/"browser_download_url": "//'

# Refresh the rolling "latest" prerelease (stable URL, independent of GitHub's
# release-asset index).
echo "==> Refreshing 'latest' prerelease"
OLD_LATEST_ID="$(curl -sS "${AUTH[@]}" "${API}/releases/tags/latest" \
  | grep -o '"id": [0-9]*' | head -1 | sed 's/"id": //')"
if [ -n "${OLD_LATEST_ID:-}" ]; then
  curl -sS -o /dev/null -X DELETE "${AUTH[@]}" "${API}/releases/${OLD_LATEST_ID}"
fi
curl -sS -o /dev/null -X DELETE "${AUTH[@]}" \
  "https://api.github.com/repos/${OWNER}/${REL_REPO}/git/refs/tags/latest" || true
curl -sS -X POST "${AUTH[@]}" "${API}/releases" \
  -d "{\"tag_name\":\"latest\",\"target_commitish\":\"main\",\"name\":\"Latest build (auto-updated)\",\"body\":\"Always points to the newest APK: releases/download/latest/medremind.apk\",\"draft\":false,\"prerelease\":true}" \
  > /tmp/latest.json
LATEST_UPLOAD="$(grep -o '"upload_url": "[^"]*' /tmp/latest.json | sed 's/"upload_url": "//' | sed 's/{.*//')"
upload_asset "${LATEST_UPLOAD}" "medremind.apk" > /dev/null
echo "    https://github.com/${OWNER}/${REL_REPO}/releases/download/latest/medremind.apk"

# Publish the APK on an "apk" branch so Obtainium can track it with its
# "Direct APK Link" source (a raw.githubusercontent.com URL, not github.com,
# so it never touches GitHub's release-list API):
#   https://raw.githubusercontent.com/<owner>/<rel_repo>/apk/medremind.apk
echo "==> Publishing direct APK link"
PUB_TMP="$(mktemp -d)"
(
  cd "$PUB_TMP"
  git init -q
  git checkout -q -b apk
  cp "${ROOT}/${APK}" medremind.apk
  printf 'MedRemind direct APK\n' > index.html
  git add -A
  git -c user.name="${GIT_NAME:-$OWNER}" \
      -c user.email="${GIT_EMAIL:-$OWNER@users.noreply.github.com}" \
      commit -q -m "Publish medremind.apk ${TAG}"
  git -c credential.helper= push -f \
    "https://x-access-token:${GH_TOKEN}@github.com/${OWNER}/${REL_REPO}.git" apk:apk
)
rm -rf "$PUB_TMP"
echo "    https://raw.githubusercontent.com/${OWNER}/${REL_REPO}/apk/medremind.apk"

echo "==> Done"
