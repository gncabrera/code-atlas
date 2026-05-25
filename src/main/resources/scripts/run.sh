#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

if [ ! -f "app/code-atlas.jar" ]; then
  echo "Missing app/code-atlas.jar" >&2
  exit 1
fi

mkdir -p "data/logs"

if [ "${ATLAS_SKIP_UPDATE:-0}" != "1" ]; then
  sh "./update.sh" --silent || true
fi

JAVA_CMD="./app/runtime/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" "-Datlas.data.dir=$SCRIPT_DIR/data" -jar "$SCRIPT_DIR/app/code-atlas.jar"
