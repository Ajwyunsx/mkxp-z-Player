# Third-party Source

- `mkxp-z` is pinned as a Git submodule from `https://github.com/mkxp-z/mkxp-z`.
- RPG Maker games, RTP assets, fonts, and commercial resources are not vendored here.
- Full Android runtime linkage should keep the public `:engine` Kotlin API stable and replace only the native implementation behind `MkxpNative.nativeStart`.
