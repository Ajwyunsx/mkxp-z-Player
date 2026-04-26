# mkxp-z Native Runtime Boundary

The launcher calls the engine through `MkxpLauncher.launch(...)`, which starts `GameActivity` in the `:game` process and passes:

- `gameId`
- `gamePath`
- `configPath`
- `debug`

`MkxpLauncher` checks whether the packaged APK contains the full SDL/mkxp-z runtime libraries. If they exist, it starts `SdlGameActivity`; otherwise it starts `GameActivity`, which invokes `MkxpNative.start(configPath, gamePath, debug)` and shows a diagnostic fallback.

The existing Android port at `https://github.com/thehatkid/mkxp-z-android` is the reference for SDL Java glue, `Android.mk`, dependency versions, and gamepad behavior. Its build scaffold is staged in `engine/src/main/jni`. The pinned upstream engine source lives in `third_party/mkxp-z`.

The app layer already writes mkxp-z-compatible JSON config keys such as `gameFolder`, `rgssVersion`, `fullscreen`, `fixedAspectRatio`, `smoothScaling`, `fixedFramerate`, `displayFPS`, `printFPS`, `subImageFix`, and `midiSoundFont`.

Important launch detail: Android mkxp-z reads `mkxp.json` from its current working directory. `SdlGameActivity.GAME_PATH` therefore points to the generated config directory, not necessarily the game directory. The JSON `gameFolder` entry points to the actual RPG Maker game path.
