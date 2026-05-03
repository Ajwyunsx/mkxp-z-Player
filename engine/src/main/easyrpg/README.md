# EasyRPG Android Core

The launcher uses `org.easyrpg.player.player.EasyRpgPlayerActivity` as a JNI
compatibility bridge because upstream EasyRPG Android native code looks up that
exact class name.

Runtime activation is automatic:

1. Build or copy `libeasyrpg_android.so` into `engine/src/main/jniLibs/<abi>/`.
2. Keep `libSDL2.so` packaged for the same ABI.
3. The app detects `libeasyrpg_android.so` at launch time and routes RPG Maker
   2000/2003 games to the internal EasyRPG core.

Without `libeasyrpg_android.so`, 2000/2003 games open a safe fallback screen
instead of being sent to mkxp-z.

Use `build_easyrpg_android.ps1` after installing EasyRPG buildscripts:

```powershell
$env:EASYRPG_BUILDSCRIPTS = "C:\path\to\buildscripts"
powershell -ExecutionPolicy Bypass -File engine\src\main\easyrpg\build_easyrpg_android.ps1 -CopyToEngineJniLibs
```

Upstream Android build docs point to https://github.com/EasyRPG/buildscripts for
the prebuilt dependency toolchain required by EasyRPG's CMake project.

`third_party/easyrpg-player/lib/liblcf` is also vendored from the liblcf 0.8.1
source zip. The build script enables `PLAYER_BUILD_LIBLCF` by default so CMake
does not need to clone liblcf during configure.
