#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

MODE="${1:-debug}"
case "$MODE" in
  debug) GRADLE_TASK="assembleDebug" ;;
  release) GRADLE_TASK="assembleRelease" ;;
  clean)
    echo "Cleaning AndroidEw project..."
    ./gradlew clean || true
    rm -rf ew/target app/src/main/jniLibs
    echo "Clean finished."
    exit 0
    ;;
  *)
    echo "Usage: ./build_android_ew.sh [debug|release|clean]" >&2
    exit 2
    ;;
esac

echo "[AndroidEw] Mode: $MODE"

for cmd in cargo rustup java; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: $cmd was not found. Install Rust/JDK/Android Studio first." >&2
    exit 1
  fi
done

if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "cargo-ndk was not found. Installing it now..."
  cargo install cargo-ndk
fi

rustup target add aarch64-linux-android

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  if [[ -n "${ANDROID_NDK_ROOT:-}" ]]; then
    export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
  else
    candidates=()
    [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME/ndk" ]] && candidates+=("$ANDROID_HOME/ndk")
    [[ -n "${ANDROID_SDK_ROOT:-}" && -d "$ANDROID_SDK_ROOT/ndk" ]] && candidates+=("$ANDROID_SDK_ROOT/ndk")
    [[ -d "$HOME/Android/Sdk/ndk" ]] && candidates+=("$HOME/Android/Sdk/ndk")
    [[ -d "$HOME/Library/Android/sdk/ndk" ]] && candidates+=("$HOME/Library/Android/sdk/ndk")
    if [[ ${#candidates[@]} -gt 0 ]]; then
      export ANDROID_NDK_HOME="$(find "${candidates[@]}" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
    fi
  fi
fi

if [[ -z "${ANDROID_NDK_HOME:-}" || ! -d "$ANDROID_NDK_HOME" ]]; then
  echo "ERROR: Android NDK was not found." >&2
  echo "Install NDK in Android Studio SDK Manager, or export ANDROID_NDK_HOME=/path/to/ndk." >&2
  exit 1
fi
export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
echo "Using ANDROID_NDK_HOME=$ANDROID_NDK_HOME"

if [[ "$MODE" == "release" ]]; then
  if [[ ! -f app/lovelive.keystore ]]; then
    echo "ERROR: Release build needs app/lovelive.keystore." >&2
    echo "Generate one with:" >&2
    echo "  keytool -genkeypair -v -keystore app/lovelive.keystore -alias androidewcdn -keyalg RSA -keysize 2048 -validity 10000" >&2
    exit 1
  fi
  for var in RELEASE_KEYSTORE_PASSWORD RELEASE_KEYSTORE_ALIAS RELEASE_KEY_PASSWORD; do
    if [[ -z "${!var:-}" ]]; then
      echo "ERROR: Set $var before release build." >&2
      exit 1
    fi
  done
fi

echo "[1/3] Enabling cdylib output in ew/Cargo.toml..."
python3 - <<'PY_ANDROID_SCRIPT'
from pathlib import Path
p = Path('ew/Cargo.toml')
s = p.read_text(encoding='utf-8')
s = s.replace('#[lib]', '[lib]').replace('#crate-type', 'crate-type').replace('#required-features', 'required-features')
p.write_text(s, encoding='utf-8')
PY_ANDROID_SCRIPT


if [ ! -f "ew/webui/index.html" ]; then
  echo "[AndroidEw] ew webui submodule is missing; creating a minimal placeholder ew/webui/index.html for source-zip builds."
  mkdir -p ew/webui
  cat > ew/webui/index.html <<'EOF'
<!doctype html>
<html><head><meta charset="utf-8"><title>ew WebUI placeholder</title></head><body>ew WebUI placeholder</body></html>
EOF
fi

echo "[2/3] Building Rust native library libew.so..."
(
  cd ew
  cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs build --release --features library
)
[[ -f app/src/main/jniLibs/arm64-v8a/libew.so ]] || { echo "ERROR: libew.so was not created." >&2; exit 1; }

echo "[3/3] Building Android APK with Gradle: $GRADLE_TASK..."
chmod +x ./gradlew
./gradlew "$GRADLE_TASK"

echo
echo "Build finished."
if [[ "$MODE" == "debug" ]]; then
  echo "Output: $PWD/app/build/outputs/apk/debug/app-debug.apk"
else
  echo "Output: $PWD/app/build/outputs/apk/release/app-release.apk"
fi
