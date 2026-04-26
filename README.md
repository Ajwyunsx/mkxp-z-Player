# mkxp-z RPG Maker Player

Android launcher for RPG Maker XP/VX/VX Ace/MV/MZ games, written in Kotlin with Jetpack Compose and a Material 3 UI. The launcher stores games, writes per-game `mkxp.json` files for RGSS games, and starts a separate full-screen game process through the engine module. XP/VX/VX Ace run through mkxp-z; MV/MZ run through the Android WebView HTML5 runtime.

## Current shape

- `:app` contains the portrait launcher, Room database, DataStore settings, game import flow, theme controls, and launch orchestration.
- `:engine` contains the Android game activities, JNI boundary, mkxp-z native bridge, and WebView runtime for MV/MZ.
- `third_party/mkxp-z` is reserved for the pinned upstream mkxp-z source.

Android 12/API 31 and newer can use official Material dynamic colors. Android 11/API 30 falls back to the user-selected seed color.

## Build

```powershell
.\gradlew assembleDebug
.\gradlew testDebugUnitTest
```

The default native library is a small bridge that validates the launcher-to-engine boundary. Building the full mkxp-z Android runtime requires the SDL/Ruby/OpenAL dependency build used by the Android port and should be wired behind the existing `:engine` API so the app layer does not change.

## License

This project is intended to be distributed under GPL-compatible terms because it integrates mkxp-z. It does not include RPG Maker games, RTP assets, or commercial resources.
