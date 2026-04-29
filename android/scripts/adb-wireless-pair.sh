#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"

find_adb_bin() {
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/platform-tools/adb" ]]; then
    printf '%s\n' "${ANDROID_SDK_ROOT}/platform-tools/adb"
    return
  fi

  if [[ -f "$ANDROID_DIR/local.properties" ]]; then
    local sdk_dir
    sdk_dir="$(sed -n 's/^sdk.dir=//p' "$ANDROID_DIR/local.properties" | tail -n1 | sed 's#\\:#:#g; s#\\\\#/#g')"
    if [[ -n "$sdk_dir" && -x "$sdk_dir/platform-tools/adb" ]]; then
      printf '%s\n' "$sdk_dir/platform-tools/adb"
      return
    fi
  fi

  if [[ -x "$HOME/Android/Sdk/platform-tools/adb" ]]; then
    printf '%s\n' "$HOME/Android/Sdk/platform-tools/adb"
    return
  fi

  if command -v adb >/dev/null 2>&1; then
    command -v adb
    return
  fi

  return 1
}

usage() {
  cat <<'HELP'
Usage:
  android/scripts/adb-wireless-pair.sh HOST:PAIR_PORT PAIR_CODE [--connect HOST:DEBUG_PORT]
  android/scripts/adb-wireless-pair.sh --host HOST:PAIR_PORT --code PAIR_CODE [--connect HOST:DEBUG_PORT]

Examples:
  android/scripts/adb-wireless-pair.sh 10.254.77.55:42843 830506
  android/scripts/adb-wireless-pair.sh --host 192.168.1.23:37143 --code 123456 --connect 192.168.1.23:38355

Notes:
- HOST:PAIR_PORT comes from "使用配对码配对设备" / "Pair device with pairing code".
- --connect is optional and should use the separate debug address shown on the Wireless debugging screen.
- Phone and computer must stay on the same LAN/hotspot. Public Wi‑Fi often blocks adb peer traffic.
HELP
}

PAIR_ENDPOINT="${ADB_PAIR_HOST:-}"
PAIR_CODE="${ADB_PAIR_CODE:-}"
CONNECT_ENDPOINT="${ADB_CONNECT_HOST:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --host)
      PAIR_ENDPOINT="${2:-}"
      shift 2
      ;;
    --code)
      PAIR_CODE="${2:-}"
      shift 2
      ;;
    --connect)
      CONNECT_ENDPOINT="${2:-}"
      shift 2
      ;;
    *)
      if [[ -z "$PAIR_ENDPOINT" ]]; then
        PAIR_ENDPOINT="$1"
      elif [[ -z "$PAIR_CODE" ]]; then
        PAIR_CODE="$1"
      else
        echo "Unknown argument: $1" >&2
        usage >&2
        exit 1
      fi
      shift
      ;;
  esac
done

if [[ -z "$PAIR_ENDPOINT" || -z "$PAIR_CODE" ]]; then
  usage >&2
  exit 1
fi

if [[ ! "$PAIR_ENDPOINT" =~ : ]]; then
  echo "Pair endpoint must be HOST:PORT, got: $PAIR_ENDPOINT" >&2
  exit 1
fi

if [[ -n "$CONNECT_ENDPOINT" && ! "$CONNECT_ENDPOINT" =~ : ]]; then
  echo "Connect endpoint must be HOST:PORT, got: $CONNECT_ENDPOINT" >&2
  exit 1
fi

ADB_BIN="$(find_adb_bin)"

echo "[adb-wireless-pair] using adb: $ADB_BIN"
echo "[adb-wireless-pair] pairing with $PAIR_ENDPOINT"
"$ADB_BIN" start-server >/dev/null
"$ADB_BIN" pair "$PAIR_ENDPOINT" "$PAIR_CODE"

echo "[adb-wireless-pair] pair command finished"

if [[ -n "$CONNECT_ENDPOINT" ]]; then
  echo "[adb-wireless-pair] connecting to $CONNECT_ENDPOINT"
  "$ADB_BIN" connect "$CONNECT_ENDPOINT"
else
  cat <<HELP
[adb-wireless-pair] pairing completed.
Next step (if needed):
  $ADB_BIN connect <phone-debug-ip:port>
Use the separate "IP 地址和端口" / "IP address & port" value from the Wireless debugging main screen.
HELP
fi

echo "[adb-wireless-pair] current adb devices:"
"$ADB_BIN" devices -l
