# mkxp-z Android Runtime Build Scaffold

This directory contains the Android `ndk-build` runtime scaffold from the mkxp-z Android port. It is intentionally not wired into the default Gradle build yet; the default build keeps using the small CMake bridge so the launcher remains buildable on a clean Windows workstation.

To build the full runtime:

1. Install a Unix-like shell environment with `bash`, `make`, `wget`, `tar`, `git`, `pkg-config`, and Android NDK/CMake access.
2. Run `get_deps.sh` from this directory to fetch SDL2, SDL2_image, SDL2_ttf, SDL2_sound, Ruby, OpenAL, OpenSSL, PhysFS, and codec libraries.
3. Use `Makefile` to build dependency outputs into `build-arm64-v8a` and `build-armeabi-v7a`.
4. Switch `:engine` from `src/main/cpp/CMakeLists.txt` to this directory's `Android.mk`.
5. Keep the Kotlin launch contract unchanged: `SdlGameActivity.GAME_PATH` must point to the directory containing `mkxp.json`, while `gameFolder` in that JSON points to the real game directory.

Rendering fixes already applied on the Java side:

- SDL surface is added with `MATCH_PARENT` layout params instead of relying on default `RelativeLayout` params.
- SDL root layout is black to prevent transparent/flickering edges during surface creation.
- Surface holder is forced to opaque.
- Invalid 0-sized surface callbacks are ignored.
