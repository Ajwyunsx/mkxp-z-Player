# Third-party Source

- `mkxp-z` is pinned as a Git submodule from `https://github.com/mkxp-z/mkxp-z`.
- `easyrpg-player` is vendored from the EasyRPG Player 0.8.1.1 GitHub source
  zip for RPG Maker 2000/2003 support.
- `easyrpg-player/lib/liblcf` is vendored from the liblcf 0.8.1 GitHub source
  zip so EasyRPG's CMake path does not need a Git clone for liblcf.
- `uk.co.armedpineapple.innoextract:service` is consumed from Maven Central to
  unpack the official VX RTP Inno Setup installer on Android.
- RPG Maker games, RTP assets, fonts, and commercial resources are not vendored here.
- Full Android runtime linkage should keep the public `:engine` Kotlin API stable and replace only the native implementation behind `MkxpNative.nativeStart`.
