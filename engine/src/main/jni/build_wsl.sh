#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SRC_DIR/../../../.." && pwd)"
BUILD_ROOT="${MKXP_WSL_BUILD_ROOT:-/root/mkxp-z-android-jni}"
NDK_HOME="${ANDROID_NDK_HOME:-/root/Android/Sdk/ndk/25.1.8937393}"
API="${ANDROID_API:-30}"
LOG_DIR="${MKXP_NATIVE_LOG_DIR:-$REPO_DIR/engine/build/native-logs}"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$LOG_DIR"

run_logged() {
  local name="$1"
  shift
  local log_file="$LOG_DIR/${STAMP}-${name}.log"

  echo "==> $name"
  echo "    log: $log_file"
  (
    set -euo pipefail
    cd "$BUILD_ROOT"
    "$@"
  ) 2>&1 | tee "$log_file"

  local status="${PIPESTATUS[0]}"
  if [[ "$status" != "0" ]]; then
    echo "FAILED: $name"
    echo "Log: $log_file"
    exit "$status"
  fi
}

copy_sources() {
  case "$BUILD_ROOT" in
    /root/mkxp-z-android-jni|/root/mkxp-z-android-jni/*|/root/mkxp-z-android-jni-*) ;;
    *)
      echo "Refusing to clean unexpected build root: $BUILD_ROOT" >&2
      exit 2
      ;;
  esac

  echo "==> prepare-build-root"
  echo "    source: $SRC_DIR"
  echo "    target: $BUILD_ROOT"
  if [[ "${MKXP_WSL_RESUME:-0}" == "1" && -d "$BUILD_ROOT" ]]; then
    echo "    resume: preserving existing dependency/build directories"
    find "$SRC_DIR" -maxdepth 1 -type f -exec cp -a {} "$BUILD_ROOT"/ \;
    mkdir -p "$BUILD_ROOT/mkxp-z" "$BUILD_ROOT/preconfigured" "$BUILD_ROOT/patches"
    cp -a "$SRC_DIR/mkxp-z"/. "$BUILD_ROOT/mkxp-z"/
    cp -a "$SRC_DIR/preconfigured"/. "$BUILD_ROOT/preconfigured"/
    cp -a "$SRC_DIR/patches"/. "$BUILD_ROOT/patches"/
  else
    rm -rf "$BUILD_ROOT"
    mkdir -p "$BUILD_ROOT"
    find "$SRC_DIR" -maxdepth 1 -type f -exec cp -a {} "$BUILD_ROOT"/ \;
    cp -a "$SRC_DIR/mkxp-z" "$BUILD_ROOT"/
    cp -a "$SRC_DIR/preconfigured" "$BUILD_ROOT"/
    cp -a "$SRC_DIR/patches" "$BUILD_ROOT"/
  fi
  find "$BUILD_ROOT" -maxdepth 1 -type f \( -name "*.sh" -o -name "*.mk" -o -name "Makefile" \) -exec sed -i 's/\r$//' {} +
  find "$BUILD_ROOT/mkxp-z" -type f -name "*.sh" -exec sed -i 's/\r$//' {} +
  chmod +x "$BUILD_ROOT/get_deps.sh" "$BUILD_ROOT/build_wsl.sh" || true
  chmod +x "$BUILD_ROOT/mkxp-z/make_xxd.sh" || true
  echo "    copied build scripts, mkxp-z source, and preconfigured files"
}

generate_xxd() {
  if ! command -v xxd >/dev/null 2>&1; then
    echo "xxd not found in WSL. Install package vim-common or xxd." >&2
    exit 1
  fi

  cd "$BUILD_ROOT/mkxp-z"
  bash ./make_xxd.sh
}

deps_for_abi() {
  local abi="$1"
  case "$abi" in
    arm64-v8a)
      env ANDROID_NDK_HOME="$NDK_HOME" ARCH=linux-x86_64 API="$API" \
        ABI=arm64-v8a HOST=aarch64-linux-android TARGET=aarch64-linux-android \
        make
      ;;
    armeabi-v7a)
      env ANDROID_NDK_HOME="$NDK_HOME" ARCH=linux-x86_64 API="$API" \
        ABI=armeabi-v7a HOST=armv7a-linux-androideabi TARGET=arm-linux-androideabi \
        make
      ;;
    *)
      echo "Unsupported ABI: $abi" >&2
      exit 2
      ;;
  esac
}

ndk_for_abi() {
  local abi="$1"
  env ANDROID_NDK_HOME="$NDK_HOME" "$NDK_HOME/ndk-build" \
    NDK_PROJECT_PATH=. \
    NDK_APPLICATION_MK=Application.mk \
    APP_BUILD_SCRIPT=Android.mk \
    APP_PLATFORM="android-$API" \
    APP_ABI="$abi" \
    V=1 \
    -j"$(nproc)"
}

copy_libs_for_abi() {
  local abi="$1"
  local out_dir="$SRC_DIR/../jniLibs/$abi"
  mkdir -p "$out_dir"

  if [[ ! -d "$BUILD_ROOT/libs/$abi" ]]; then
    echo "NDK output missing: $BUILD_ROOT/libs/$abi" >&2
    exit 1
  fi

  cp -a "$BUILD_ROOT/libs/$abi"/. "$out_dir"/
  echo "Copied $abi libraries to $out_dir"
}

if [[ ! -x "$NDK_HOME/ndk-build" ]]; then
  echo "Linux Android NDK not found at: $NDK_HOME" >&2
  exit 1
fi

abis=("$@")
if [[ "${#abis[@]}" == "0" ]]; then
  abis=(arm64-v8a armeabi-v7a)
fi

copy_sources
run_logged get-deps bash "$BUILD_ROOT/get_deps.sh"
run_logged generate-xxd generate_xxd

for abi in "${abis[@]}"; do
  run_logged "deps-$abi" deps_for_abi "$abi"
  run_logged "ndk-$abi" ndk_for_abi "$abi"
  run_logged "copy-libs-$abi" copy_libs_for_abi "$abi"
done

echo "Native build finished. Logs are in: $LOG_DIR"
