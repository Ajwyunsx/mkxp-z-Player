#!/usr/bin/env bash
set -euo pipefail

# This script downloads/git clones project dependencies such as libogg, SDL2,
# Ruby, etc. It is intentionally resumable because network failures can leave
# partial archives or incomplete Git directories on WSL.

GIT_ARGS=(
  -q
  -c advice.detachedHead=false
  -c http.version=HTTP/1.1
  -c http.lowSpeedLimit=1024
  -c http.lowSpeedTime=60
  --single-branch
  --depth 1
)

retry() {
  local attempt max_attempts
  max_attempts="$1"
  shift

  for attempt in $(seq 1 "$max_attempts"); do
    if "$@"; then
      return 0
    fi

    if [[ "$attempt" == "$max_attempts" ]]; then
      return 1
    fi

    echo "Retrying ($attempt/$max_attempts): $*"
    sleep 2
  done
}

ensure_clean_target() {
  local target marker
  target="$1"
  marker="$2"

  if [[ -d "$target" && -e "$target/$marker" ]]; then
    return 0
  fi

  if [[ -e "$target" ]]; then
    echo "Removing incomplete dependency directory: $target"
    rm -rf "$target"
  fi

  return 1
}

clone_dep() {
  local target branch repo
  target="$1"
  branch="$2"
  repo="$3"
  shift 3

  if [[ -d "$target/.git" ]] && git -C "$target" rev-parse --verify HEAD >/dev/null 2>&1; then
    return 0
  fi

  if [[ -e "$target" ]]; then
    echo "Removing incomplete dependency directory: $target"
    rm -rf "$target"
  fi

  echo "Downloading $target..."
  retry 3 git clone "${GIT_ARGS[@]}" "$@" -b "$branch" "$repo" "$target"
}

download_archive() {
  local output url
  output="$1"
  shift

  rm -f "$output"
  for url in "$@"; do
    echo "Fetching $url"
    if retry 3 wget -q --show-progress --progress=dot:giga --tries=3 --timeout=30 --read-timeout=60 -O "$output.tmp" "$url"; then
      mv "$output.tmp" "$output"
      return 0
    fi
    rm -f "$output.tmp"
  done

  echo "Failed to download $output" >&2
  return 1
}

extract_archive_dep() {
  local target marker archive extracted
  target="$1"
  marker="$2"
  archive="$3"
  extracted="$4"
  shift 4

  if [[ -d "$target/.git" ]]; then
    echo "Replacing Git checkout with archive dependency: $target"
    rm -rf "$target"
  fi

  if ensure_clean_target "$target" "$marker"; then
    return 0
  fi

  echo "Downloading $target..."
  download_archive "$archive" "$@"
  local actual_extracted
  actual_extracted="$(tar -tf "$archive" | sed -n '1s,/.*,,p')"
  tar -xf "$archive"
  rm -rf "$target"
  if [[ -d "$extracted" ]]; then
    mv "$extracted" "$target"
  elif [[ -n "$actual_extracted" && -d "$actual_extracted" ]]; then
    mv "$actual_extracted" "$target"
  else
    echo "Unable to find extracted directory for $archive" >&2
    return 1
  fi
  rm -f "$archive"
}

apply_patch_once() {
  local name check_file check_pattern patch_file
  name="$1"
  check_file="$2"
  check_pattern="$3"
  patch_file="$4"

  if grep -q "$check_pattern" "$check_file"; then
    return 0
  fi

  echo "Applying $name..."
  patch -p0 < "$patch_file"
}

# Xiph libogg
extract_archive_dep \
  libogg configure libogg-1.3.5.tar.gz libogg-1.3.5 \
  https://downloads.xiph.org/releases/ogg/libogg-1.3.5.tar.gz \
  https://ftp.osuosl.org/pub/xiph/releases/ogg/libogg-1.3.5.tar.gz

# Xiph libvorbis
extract_archive_dep \
  libvorbis configure libvorbis-1.3.7.tar.gz libvorbis-1.3.7 \
  https://downloads.xiph.org/releases/vorbis/libvorbis-1.3.7.tar.gz \
  https://ftp.osuosl.org/pub/xiph/releases/vorbis/libvorbis-1.3.7.tar.gz

# Xiph libtheora
extract_archive_dep \
  libtheora configure.ac libtheora-1.1.1.tar.gz libtheora-1.1.1 \
  https://downloads.xiph.org/releases/theora/libtheora-1.1.1.tar.gz \
  https://ftp.osuosl.org/pub/xiph/releases/theora/libtheora-1.1.1.tar.gz

# GNU libiconv
extract_archive_dep \
  libiconv configure libiconv-1.17.tar.gz libiconv-1.17 \
  https://ftp.gnu.org/gnu/libiconv/libiconv-1.17.tar.gz \
  https://mirrors.kernel.org/gnu/libiconv/libiconv-1.17.tar.gz \
  https://ftpmirror.gnu.org/gnu/libiconv/libiconv-1.17.tar.gz

# Freedesktop uchardet
# GitHub's mirror is often very slow from WSL on this machine, so use the
# official freedesktop GitLab archive instead of a Git clone.
extract_archive_dep \
  uchardet CMakeLists.txt uchardet-v0.0.8.tar.gz uchardet-v0.0.8 \
  https://gitlab.freedesktop.org/uchardet/uchardet/-/archive/v0.0.8/uchardet-v0.0.8.tar.gz

# Freedesktop Pixman
extract_archive_dep \
  pixman configure pixman-0.42.2.tar.gz pixman-0.42.2 \
  https://www.cairographics.org/releases/pixman-0.42.2.tar.gz \
  https://gitlab.freedesktop.org/pixman/pixman/-/archive/pixman-0.42.2/pixman-pixman-0.42.2.tar.gz

# PhysicsFS
extract_archive_dep \
  physfs CMakeLists.txt physfs-3.2.0.tar.bz2 physfs-3.2.0 \
  https://icculus.org/physfs/downloads/physfs-3.2.0.tar.bz2 \
  https://github.com/icculus/physfs/archive/refs/tags/release-3.2.0.tar.gz

# OpenAL Soft 1.23.0
extract_archive_dep \
  openal CMakeLists.txt openal-soft-1.23.0.tar.bz2 openal-soft-1.23.0 \
  https://openal-soft.org/openal-releases/openal-soft-1.23.0.tar.bz2 \
  https://github.com/kcat/openal-soft/archive/refs/tags/1.23.0.tar.gz

# SDL2
extract_archive_dep \
  SDL2 Android.mk SDL2-2.26.3.tar.gz SDL2-2.26.3 \
  https://github.com/libsdl-org/SDL/releases/download/release-2.26.3/SDL2-2.26.3.tar.gz \
  https://www.libsdl.org/release/SDL2-2.26.3.tar.gz
apply_patch_once \
  "SDL2 Android RGSS-thread GL resume patch" \
  SDL2/src/video/android/SDL_androidevents.c \
  "current_context = SDL_GL_GetCurrentContext" \
  patches/sdl2-android-rgss-thread-gl-resume.patch
apply_patch_once \
  "SDL2 Android surface destroy wait patch" \
  SDL2/src/core/android/SDL_android.c \
  "int nb_attempt = 250" \
  patches/sdl2-android-surface-destroy-wait.patch

# SDL2_image
extract_archive_dep \
  SDL2_image Android.mk SDL2_image-2.6.3.tar.gz SDL2_image-2.6.3 \
  https://github.com/libsdl-org/SDL_image/releases/download/release-2.6.3/SDL2_image-2.6.3.tar.gz \
  https://www.libsdl.org/projects/SDL_image/release/SDL2_image-2.6.3.tar.gz

# SDL2_ttf
extract_archive_dep \
  SDL2_ttf Android.mk SDL2_ttf-2.20.2.tar.gz SDL2_ttf-2.20.2 \
  https://github.com/libsdl-org/SDL_ttf/releases/download/release-2.20.2/SDL2_ttf-2.20.2.tar.gz \
  https://www.libsdl.org/projects/SDL_ttf/release/SDL2_ttf-2.20.2.tar.gz

# SDL2_sound
extract_archive_dep \
  SDL2_sound Android.mk SDL_sound-2.0.1.tar.gz SDL_sound-2.0.1 \
  https://github.com/icculus/SDL_sound/archive/refs/tags/v2.0.1.tar.gz \
  https://icculus.org/SDL_sound/downloads/SDL_sound-2.0.1.tar.gz

# OpenSSL 1.1.1t
extract_archive_dep \
  openssl Configure openssl-1.1.1t.tar.gz openssl-1.1.1t \
  https://www.openssl.org/source/old/1.1.1/openssl-1.1.1t.tar.gz \
  https://www.openssl.org/source/openssl-1.1.1t.tar.gz

# Ruby 3.1.0 (patched for mkxp-z)
clone_dep ruby mkxp-z-3.1 https://github.com/mkxp-z/ruby

echo "Done!"
