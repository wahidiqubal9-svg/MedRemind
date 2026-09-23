#!/usr/bin/env bash
#
# Build the debug APK and publish it as a GitHub Release so Obtainium can pick it up.
#
# Usage:
#   GH_TOKEN=<token> GRADLE=/path/to/gradle ./scripts/release.sh v0.2.0 "notes"
#
set -euo pipefail

OWNER="${REPO_OWNER:-wahidiqubal9-svg}"
REPO="${REPO_NAME:-MedRemind}"
GRADLE="${GRADLE:-gradle}"
TAG="${1:?usage: release.sh <tag, e.g. v0.2.0> [notes]}"
NOTES="${2:-Automatic build $TAG}"

: "${GH_TOKEN:?GH_TOKEN environment variable is required}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

APK="app/build/outputs/apk/debug/app-debug.apk"

echo "==> Building $TAG"
"$GRADLE" --no-daemon --console=plain assembleDebug

echo "==> Staging commit"
git add -A
if ! git diff --cached --quiet; then
  git -c user.name="${GIT_NAME:-$OWNER}" \
      -c user.email="${GIT_EMAIL:-$OWNER@users.noreply.github.com}" \
      commit -m "Release $TAG"
fi
git -c credential.helper= push "https://x-access-token:${GH_TOKEN}@github.com/${OWNER}/${REPO}.git" HEAD:main

echo "==> Creating GitHub release $TAG"
curl -sS -X POST \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${OWNER}/${REPO}/releases" \
  -d "{\"tag_name\":\"${TAG}\",\"target_commitish\":\"main\",\"name\":\"${TAG}\",\"body\":\"${NOTES}\",\"draft\":false,\"prerelease\":false}" \
  > /tmp/release.json

UPLOAD_URL="$(grep -o '"upload_url": "[^"]*' /tmp/release.json | sed 's/"upload_url": "//' | sed 's/{.*//')"
if [ -z "$UPLOAD_URL" ]; then
  echo "Failed to create release:"; cat /tmp/release.json; exit 1
fi

ASSET_NAME="medremind-${TAG}-debug.apk"
echo "==> Uploading ${ASSET_NAME}"
curl -sS -X POST \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@${APK}" \
  "${UPLOAD_URL}?name=${ASSET_NAME}" > /tmp/asset.json

grep -o '"browser_download_url": "[^"]*' /tmp/asset.json | sed 's/"browser_download_url": "//'

# Also publish a stable-named copy so an update tool can track a single URL:
# https://github.com/<owner>/<repo>/releases/latest/download/medremind-latest.apk
echo "==> Uploading medremind-latest.apk"
curl -sS -X POST \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@${APK}" \
  "${UPLOAD_URL}?name=medremind-latest.apk" > /tmp/asset_latest.json

grep -o '"browser_download_url": "[^"]*' /tmp/asset_latest.json | sed 's/"browser_download_url": "//'

# Refresh the stable "latest" prerelease so this URL always serves the newest APK,
# independent of GitHub's release-asset index:
#   https://github.com/<owner>/<repo>/releases/download/latest/medremind.apk
echo "==> Refreshing 'latest' prerelease"
OLD_LATEST_ID="$(curl -sS -H "Authorization: Bearer ${GH_TOKEN}" -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${OWNER}/${REPO}/releases/tags/latest" \
  | grep -o '"id": [0-9]*' | head -1 | sed 's/"id": //')"
if [ -n "${OLD_LATEST_ID:-}" ]; then
  curl -sS -o /dev/null -X DELETE -H "Authorization: Bearer ${GH_TOKEN}" \
    "https://api.github.com/repos/${OWNER}/${REPO}/releases/${OLD_LATEST_ID}"
fi
curl -sS -o /dev/null -X DELETE -H "Authorization: Bearer ${GH_TOKEN}" \
  "https://api.github.com/repos/${OWNER}/${REPO}/git/refs/tags/latest" || true
curl -sS -X POST -H "Authorization: Bearer ${GH_TOKEN}" -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${OWNER}/${REPO}/releases" \
  -d "{\"tag_name\":\"latest\",\"target_commitish\":\"main\",\"name\":\"Latest build (auto-updated)\",\"body\":\"Always points to the newest APK: releases/download/latest/medremind.apk\",\"draft\":false,\"prerelease\":true}" \
  -o /tmp/latest.json
LATEST_UPLOAD="$(grep -o '"upload_url": "[^"]*' /tmp/latest.json | sed 's/"upload_url": "//' | sed 's/{.*//')"
curl -sS -X POST -H "Authorization: Bearer ${GH_TOKEN}" -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@${APK}" "${LATEST_UPLOAD}?name=medremind.apk" > /dev/null

echo "==> Done"
