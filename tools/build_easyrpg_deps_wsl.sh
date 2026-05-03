#!/usr/bin/env bash
set -euo pipefail

real_root="/mnt/c/Users/34275/Documents/New project 2/third_party/easyrpg-buildscripts"
work_root="${EASYRPG_BUILD_WORK_ROOT:-/tmp/mkxpz-easyrpg-buildscripts-work}"
mkdir -p "$work_root"
cp -a "$real_root/." "$work_root/"
find "$work_root" -type f \( -name "*.sh" -o -name "*.patch" \) -exec sed -i 's/\r$//' {} +

cd "$work_root/android"
chmod +x ./*.sh ../shared/*.sh
export EASYRPG_ANDROID_ABIS="${EASYRPG_ANDROID_ABIS:-armeabi-v7a,arm64-v8a}"
export EASYRPG_INSTALL_LEGACY_M2REPO="${EASYRPG_INSTALL_LEGACY_M2REPO:-0}"

bash ./1_download_library.sh
bash ./2_build_toolchain.sh

for abi in armeabi-v7a arm64-v8a x86 x86_64; do
  if [ -d "$abi-toolchain" ]; then
    rm -rf "$real_root/android/$abi-toolchain"
    cp -a "$abi-toolchain" "$real_root/android/"
  fi
done
