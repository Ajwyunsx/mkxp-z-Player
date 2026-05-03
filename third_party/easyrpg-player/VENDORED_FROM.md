# EasyRPG Player Source

- Upstream: https://github.com/EasyRPG/Player
- Source archive: https://codeload.github.com/EasyRPG/Player/zip/refs/tags/0.8.1.1
- Version: 0.8.1.1
- Download log: `build-logs/github-easyrpg-source-zip-20260501.log`

This directory is vendored from the GitHub source zip, not a Git submodule. The
Android native build still requires EasyRPG's Android dependency toolchain from
https://github.com/EasyRPG/buildscripts.

## Bundled liblcf

- Upstream: https://github.com/EasyRPG/liblcf
- Source archive: https://codeload.github.com/EasyRPG/liblcf/zip/refs/tags/0.8.1
- Version: 0.8.1
- Path: `lib/liblcf`
- Download log: `build-logs/github-easyrpg-liblcf-source-zip-20260501.log`

`lib/liblcf` is included from a source zip so Android CMake builds can use
`-DPLAYER_BUILD_LIBLCF=ON` without cloning from GitHub during configure.
