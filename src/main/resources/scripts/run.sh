#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

echo "[run] Starting Code Atlas from $SCRIPT_DIR"

if [ ! -f "app/code-atlas.jar" ]; then
  echo "[run] Missing app/code-atlas.jar" >&2
  exit 1
fi
echo "[run] Found app/code-atlas.jar"

mkdir -p "data/logs"
echo "[run] Data directory: $SCRIPT_DIR/data"

# shellcheck disable=SC1091
. "$SCRIPT_DIR/load-config.sh"

echo "[run] Config: port=$ATLAS_PORT, enableAutoUpdate=$ATLAS_ENABLE_AUTO_UPDATE"

if [ "${ATLAS_SKIP_UPDATE:-0}" = "1" ]; then
  echo "[run] Skipping update check (ATLAS_SKIP_UPDATE=1)"
elif [ "$ATLAS_ENABLE_AUTO_UPDATE" = "0" ]; then
  echo "[run] Skipping update check (enableAutoUpdate=false)"
else
  echo "[run] Checking for updates..."
  sh "./update.sh" || true
fi

JAVA_CMD="./app/runtime/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi
echo "[run] Launching with Java: $JAVA_CMD"

exec "$JAVA_CMD" "-Datlas.data.dir=$SCRIPT_DIR/data" "-Dserver.port=$ATLAS_PORT" -jar "$SCRIPT_DIR/app/code-atlas.jar"
