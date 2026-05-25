#!/usr/bin/env sh

ATLAS_PORT=8088
ATLAS_ENABLE_AUTO_UPDATE=1
CONFIG_FILE="${SCRIPT_DIR:-.}/data/config.yaml"

read_config_value() {
  key="$1"
  awk -F': *' -v key="$key" '
    $1 == key {
      gsub(/\r$/, "", $2)
      print $2
      exit
    }
  ' "$CONFIG_FILE"
}

ensure_config_file() {
  if [ -f "$CONFIG_FILE" ]; then
    return 0
  fi
  mkdir -p "$(dirname "$CONFIG_FILE")"
  echo "[run] Creating default config: $CONFIG_FILE"
  cat >"$CONFIG_FILE" <<'EOF'
port: 8088
enableAutoUpdate: true
EOF
}

normalize_enable_auto_update() {
  value=$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')
  case "$value" in
    false|0|no)
      ATLAS_ENABLE_AUTO_UPDATE=0
      ;;
    *)
      ATLAS_ENABLE_AUTO_UPDATE=1
      ;;
  esac
}

load_config() {
  ensure_config_file

  port_value=$(read_config_value "port" || true)
  if [ -n "${port_value:-}" ]; then
    ATLAS_PORT=$port_value
  fi

  auto_update_value=$(read_config_value "enableAutoUpdate" || true)
  if [ -n "${auto_update_value:-}" ]; then
    normalize_enable_auto_update "$auto_update_value"
  fi
}

load_config
