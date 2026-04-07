#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.yrd.emergencylanemobile"
REMOTE_APK_PATH="/data/local/tmp/emergency-lane-demo-debug.apk"

find_sdk_dir() {
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/platform-tools/adb" ]]; then
    printf '%s' "${ANDROID_SDK_ROOT}"
    return
  fi

  if [[ -f "$ANDROID_DIR/local.properties" ]]; then
    local sdk_dir
    sdk_dir="$(sed -n 's/^sdk.dir=//p' "$ANDROID_DIR/local.properties" | tail -n1 | sed 's#\\:#:#g; s#\\\\#/#g')"
    if [[ -n "$sdk_dir" && -x "$sdk_dir/platform-tools/adb" ]]; then
      printf '%s' "$sdk_dir"
      return
    fi
  fi

  if [[ -x "$HOME/Android/Sdk/platform-tools/adb" ]]; then
    printf '%s' "$HOME/Android/Sdk"
    return
  fi

  if command -v adb >/dev/null 2>&1; then
    command -v adb | xargs dirname | xargs dirname
    return
  fi

  return 1
}

SDK_DIR="$(find_sdk_dir)"
ADB_BIN="$SDK_DIR/platform-tools/adb"
SERIAL="${ADB_SERIAL:-${ANDROID_SERIAL:-}}"
API_BASE_URL="${EMERGENCY_LANE_API_BASE_URL:-}"
CLEAN_INSTALL=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial)
      SERIAL="$2"
      shift 2
      ;;
    --api-base-url)
      API_BASE_URL="$2"
      shift 2
      ;;
    --clean)
      CLEAN_INSTALL=1
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "$SERIAL" ]]; then
  mapfile -t DEVICES < <("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')
  if [[ ${#DEVICES[@]} -eq 0 ]]; then
    echo "No authorized adb device detected. Run '$ADB_BIN devices -l' first." >&2
    exit 1
  fi
  SERIAL="${DEVICES[0]}"
fi

GRADLE_CMD=(./gradlew --no-daemon assembleDebug)
if [[ -n "$API_BASE_URL" ]]; then
  GRADLE_CMD+=("-PemergencyLaneApiBaseUrl=${API_BASE_URL}")
fi

if [[ -z "${JAVA_HOME:-}" && -d /usr/lib/jvm/java-17-openjdk ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
fi

echo "[install-debug] building debug apk for ${SERIAL}"
(cd "$ANDROID_DIR" && "${GRADLE_CMD[@]}")

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at $APK_PATH" >&2
  exit 1
fi

run_pm_install() {
  "$ADB_BIN" -s "$SERIAL" push "$APK_PATH" "$REMOTE_APK_PATH" >/dev/null
  "$ADB_BIN" -s "$SERIAL" shell "pm install -r -t '$REMOTE_APK_PATH'"
}

run_clean_install() {
  echo "[install-debug] uninstalling existing package on ${SERIAL}"
  "$ADB_BIN" -s "$SERIAL" uninstall "$PACKAGE_NAME" >/dev/null 2>&1 || true
  run_pm_install
}

print_rejection_help() {
  cat <<HELP >&2
[install-debug] device rejected the install/update request.
- Please unlock the phone and approve the vendor install/update prompt once.
- The default path will NOT uninstall the existing app automatically.
- If you intentionally want a remove-and-reinstall attempt, rerun with:
  android/scripts/install-debug.sh --serial "$SERIAL" --clean ${API_BASE_URL:+--api-base-url "$API_BASE_URL"}
- For diagnosis:
  $ADB_BIN -s "$SERIAL" shell dumpsys package $PACKAGE_NAME | sed -n '1,120p'
HELP
}

echo "[install-debug] adb install -r -t -> ${SERIAL}"
set +e
INSTALL_OUTPUT="$($ADB_BIN -s "$SERIAL" install -r -t "$APK_PATH" 2>&1)"
INSTALL_CODE=$?
set -e
echo "$INSTALL_OUTPUT"

if [[ $INSTALL_CODE -ne 0 ]]; then
  if grep -Eqi "Unable to open file|Can't open file" <<<"$INSTALL_OUTPUT"; then
    echo "[install-debug] streamed install failed; falling back to adb push + pm install"
    set +e
    FALLBACK_OUTPUT="$(run_pm_install 2>&1)"
    FALLBACK_CODE=$?
    set -e
    echo "$FALLBACK_OUTPUT"
    if [[ $FALLBACK_CODE -ne 0 ]]; then
      if grep -Eqi "INSTALL_FAILED_ABORTED|User rejected permissions" <<<"$FALLBACK_OUTPUT"; then
        if [[ $CLEAN_INSTALL -eq 1 ]]; then
          print_rejection_help
          run_clean_install || exit 1
        else
          print_rejection_help
          exit 1
        fi
      fi
      exit $FALLBACK_CODE
    fi
  elif grep -Eqi "INSTALL_FAILED_ABORTED|User rejected permissions" <<<"$INSTALL_OUTPUT"; then
    if [[ $CLEAN_INSTALL -eq 1 ]]; then
      print_rejection_help
      run_clean_install || exit 1
    else
      print_rejection_help
      exit 1
    fi
  else
    exit $INSTALL_CODE
  fi
fi

"$ADB_BIN" -s "$SERIAL" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null
"$ADB_BIN" -s "$SERIAL" shell rm -f "$REMOTE_APK_PATH" >/dev/null 2>&1 || true

echo "[install-debug] done"
