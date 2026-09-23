#!/usr/bin/env bash
#
# Build the debug APK and publish it so Obtainium can pick it up.
#
# Everything (code + releases + a stable direct-APK link) lives in one repo:
#
#   https://raw.githubusercontent.com/<owner>/MedRemind/apk/medremind.apk
#
# The stable link is on a separate "apk" branch so it always serves the newest
# build without depending on GitHub's release-list API.
#
# Usage:
#   GH_TOKEN=<token> GRADLE=/path/to/gradle ./scripts/release.sh v0.2.0 "notes"
#
set -euo pipefail

OWNER="${REPO_OWNER:-wahidiqubal9-svg}"
CODE_REPO="${CODE_REPO_NAME:-MedRemind}"
# Space-separated list of repos that receive the versioned release.
REL_REPOS="${REL_REPO_NAMES:-MedRemind}"
# Repo that additionally gets the rolling "latest" release and the direct link.
PRIMARY_REL_REPO="${REL_REPO_NAME:-MedRemind}"
GRADLE="${GRADLE:-gradle}"
TAG="${1:?usage: release.sh <tag, e.g. v0.2.0> [notes]}"
NOTES="${2:-Automatic build $TAG}"

: "${GH_TOKEN:?GH_TOKEN environment variable is required}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

APK="app/build/outputs/apk/debug/app-debug.apk"
AUTH=(-H "Authorization: Bearer ${GH_TOKEN}" -H "Accept: application/vnd.github+json")

api() { echo "https://api.github.com/repos/${OWNER}/$1"; }

upload_asset() { # $1=upload_url  $2=asset_name
  curl -sS -X POST "${AUTH[@]}" \
    -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary "@${ROOT}/${APK}" \
    "$1?name=$2"
}

if [ "${SKIP_BUILD:-0}" = "1" ]; then
  echo "==> Skipping build (SKIP_BUILD=1); using existing APK"
  [ -f "$ROOT/$APK" ] || { echo "No APK at $APK"; exit 1; }
else
  echo "==> Building $TAG"
  "$GRADLE" --no-daemon --console=plain assembleDebug
fi

echo "==> Staging commit"
git add -A
if ! git diff --cached --quiet; then
  git -c user.name="${GIT_NAME:-$OWNER}" \
      -c user.email="${GIT_EMAIL:-$OWNER@users.noreply.github.com}" \
      commit -m "Release $TAG"
fi
git -c credential.helper= push "https://x-access-token:${GH_TOKEN}@github.com/${OWNER}/${CODE_REPO}.git" HEAD:main

ASSET_NAME="medremind-${TAG}-debug.apk"

for R in $REL_REPOS; do
  echo "==> Creating release $TAG in ${OWNER}/${R}"
  curl -sS -X POST "${AUTH[@]}" "$(api "$R")/releases" \
    -d "{\"tag_name\":\"${TAG}\",\"target_commitish\":\"main\",\"name\":\"${TAG}\",\"body\":\"${NOTES}\",\"draft\":false,\"prerelease\":false}" \
    > "/tmp/release_${R}.json"

  UP="$(grep -o '"upload_url": "[^"]*' "/tmp/release_${R}.json" | sed 's/"upload_url": "//' | sed 's/{.*//')"
  if [ -z "$UP" ]; then
    echo "Failed to create release in ${R}:"; cat "/tmp/release_${R}.json"; exit 1
  fi

  echo "==> Uploading ${ASSET_NAME} to ${R}"
  # Only ONE APK asset per release, otherwise update tools ask "pick an APK".
  upload_asset "$UP" "$ASSET_NAME" | grep -o '"browser_download_url": "[^"]*' | sed 's/"browser_download_url": "//'
done

# ---------------------------------------------------------------------------
# Extra publishing in the primary release repo:
#  - rolling "latest" prerelease (stable release URL)
#  - "apk" branch (stable direct-APK URL for Obtainium's Direct APK Link source)
# ---------------------------------------------------------------------------
PAPI="$(api "$PRIMARY_REL_REPO")"

echo "==> Refreshing 'latest' prerelease in ${PRIMARY_REL_REPO}"
OLD_LATEST_ID="$(curl -sS "${AUTH[@]}" "${PAPI}/releases/tags/latest" \
  | grep -o '"id": [0-9]*' | head -1 | sed 's/"id": //')"
if [ -n "${OLD_LATEST_ID:-}" ]; then
  curl -sS -o /dev/null -X DELETE "${AUTH[@]}" "${PAPI}/releases/${OLD_LATEST_ID}"
fi
curl -sS -o /dev/null -X DELETE "${AUTH[@]}" \
  "https://api.github.com/repos/${OWNER}/${PRIMARY_REL_REPO}/git/refs/tags/latest" || true
curl -sS -X POST "${AUTH[@]}" "${PAPI}/releases" \
  -d "{\"tag_name\":\"latest\",\"target_commitish\":\"main\",\"name\":\"Latest build (auto-updated)\",\"body\":\"Always points to the newest APK: releases/download/latest/medremind.apk\",\"draft\":false,\"prerelease\":true}" \
  > /tmp/latest.json
LATEST_UPLOAD="$(grep -o '"upload_url": "[^"]*' /tmp/latest.json | sed 's/"upload_url": "//' | sed 's/{.*//')"
upload_asset "${LATEST_UPLOAD}" "medremind.apk" > /dev/null
echo "    https://github.com/${OWNER}/${PRIMARY_REL_REPO}/releases/download/latest/medremind.apk"

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
    "https://x-access-token:${GH_TOKEN}@github.com/${OWNER}/${PRIMARY_REL_REPO}.git" apk:apk
)
rm -rf "$PUB_TMP"
echo "    https://raw.githubusercontent.com/${OWNER}/${PRIMARY_REL_REPO}/apk/medremind.apk"

echo "==> Done"
