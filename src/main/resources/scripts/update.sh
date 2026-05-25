#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

echo "[update] Checking for updates..."

if [ ! -f "app/code-atlas.jar" ]; then
  echo "[update] Skipping update check: app/code-atlas.jar not found"
  exit 0
fi

if [ -n "${ATLAS_GITHUB_REPO:-}" ]; then
  REPO="$ATLAS_GITHUB_REPO"
elif [ -f "app/repo.txt" ]; then
  REPO=$(tr -d '\r\n' < "app/repo.txt")
else
  REPO="gncabrera/code-atlas"
fi
case "$REPO" in
  *'$'*|*'@'*)
    echo "[update] Invalid repository metadata; using default"
    REPO="gncabrera/code-atlas"
    ;;
esac
echo "[update] Repository: $REPO"

LOCAL_VERSION=""
if [ -f "app/version.txt" ]; then
  LOCAL_VERSION=$(tr -d '\r\n' < "app/version.txt")
fi
case "$LOCAL_VERSION" in
  *'$'*|*'@'*)
    LOCAL_VERSION=""
    ;;
esac
if [ -n "$LOCAL_VERSION" ]; then
  echo "[update] Local version: $LOCAL_VERSION"
else
  echo "[update] Local version: unknown"
fi

OS_NAME=$(uname -s)
ASSET_NAME="code-atlas-update-linux.zip"
if [ "$OS_NAME" = "Darwin" ]; then
  ASSET_NAME="code-atlas-update-macos.zip"
fi

if command -v python3 >/dev/null 2>&1; then
  PYTHON_CMD="python3"
elif command -v python >/dev/null 2>&1; then
  PYTHON_CMD="python"
else
  echo "[update] Python missing; skipping update check"
  exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[update] curl missing; skipping update check"
  exit 0
fi

if ! command -v unzip >/dev/null 2>&1; then
  echo "[update] unzip missing; skipping update check"
  exit 0
fi

API_URL="https://api.github.com/repos/$REPO/releases/latest"
echo "[update] Fetching latest release info..."
RELEASE_JSON=$(curl -fsSL -H "User-Agent: code-atlas-updater" "$API_URL" 2>/dev/null || true)
if [ -z "$RELEASE_JSON" ]; then
  echo "[update] Could not fetch latest release info; keeping current installation"
  exit 0
fi

PARSED_OUTPUT=$(printf '%s' "$RELEASE_JSON" | "$PYTHON_CMD" - "$ASSET_NAME" <<'PY'
import json
import sys

asset_primary = sys.argv[1]
asset_fallback = "code-atlas-update.zip"
payload = json.load(sys.stdin)
tag = (payload.get("tag_name") or "").strip()
if not tag:
    sys.exit(2)
asset_url = ""
for item in payload.get("assets", []):
    name = item.get("name", "")
    if name == asset_primary:
        asset_url = item.get("browser_download_url", "")
        break
if not asset_url:
    for item in payload.get("assets", []):
        if item.get("name", "") == asset_fallback:
            asset_url = item.get("browser_download_url", "")
            break
if not asset_url:
    sys.exit(3)
print(tag)
print(asset_url)
PY
) || {
  echo "[update] Could not parse latest release info; keeping current installation"
  exit 0
}

REMOTE_VERSION=$(printf '%s\n' "$PARSED_OUTPUT" | sed -n '1p')
ASSET_URL=$(printf '%s\n' "$PARSED_OUTPUT" | sed -n '2p')

if [ -z "$REMOTE_VERSION" ] || [ -z "$ASSET_URL" ]; then
  echo "[update] Update asset not found in latest release; keeping current installation"
  exit 0
fi

echo "[update] Remote version: $REMOTE_VERSION"

if [ "$LOCAL_VERSION" = "$REMOTE_VERSION" ]; then
  echo "[update] Already up to date ($LOCAL_VERSION)"
  exit 0
fi

echo "[update] Updating from $LOCAL_VERSION to $REMOTE_VERSION"

WORK_DIR="$SCRIPT_DIR/.update-tmp"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/extracted"

cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

echo "[update] Downloading update package..."
if ! curl -fsSL -H "User-Agent: code-atlas-updater" "$ASSET_URL" -o "$WORK_DIR/update.zip"; then
  echo "[update] Update download failed; keeping current installation"
  exit 1
fi
echo "[update] Download complete"

echo "[update] Extracting update package..."
if ! unzip -q "$WORK_DIR/update.zip" -d "$WORK_DIR/extracted"; then
  echo "[update] Update extraction failed; keeping current installation"
  exit 1
fi
echo "[update] Extraction complete"

NEW_APP_DIR="$WORK_DIR/extracted/app"
if [ ! -f "$NEW_APP_DIR/code-atlas.jar" ]; then
  NEW_APP_DIR=$(find "$WORK_DIR/extracted" -maxdepth 2 -type d -name app | head -n 1 || true)
fi

if [ -z "${NEW_APP_DIR:-}" ] || [ ! -f "$NEW_APP_DIR/code-atlas.jar" ]; then
  echo "[update] Update payload missing app/code-atlas.jar; keeping current installation"
  exit 1
fi

echo "[update] Applying update..."
rm -rf "$SCRIPT_DIR/app_old"
if [ -d "$SCRIPT_DIR/app/runtime" ]; then
  mv "$SCRIPT_DIR/app/runtime" "$WORK_DIR/runtime_backup"
fi

mv "$SCRIPT_DIR/app" "$SCRIPT_DIR/app_old"
if ! mv "$NEW_APP_DIR" "$SCRIPT_DIR/app"; then
  rm -rf "$SCRIPT_DIR/app"
  mv "$SCRIPT_DIR/app_old" "$SCRIPT_DIR/app"
  echo "[update] Update swap failed; rollback complete"
  exit 1
fi

if [ -d "$WORK_DIR/runtime_backup" ] && [ ! -d "$SCRIPT_DIR/app/runtime" ]; then
  mv "$WORK_DIR/runtime_backup" "$SCRIPT_DIR/app/runtime"
fi

if [ ! -f "$SCRIPT_DIR/app/repo.txt" ] && [ -f "$SCRIPT_DIR/app_old/repo.txt" ]; then
  cp "$SCRIPT_DIR/app_old/repo.txt" "$SCRIPT_DIR/app/repo.txt"
fi

rm -rf "$SCRIPT_DIR/app_old"
echo "[update] Update complete: $REMOTE_VERSION"
exit 0
