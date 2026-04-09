# SystemPlus NEWwrite (Packagable Source)

This directory is now a complete Android Gradle project that can produce an APK for Xposed/LSPosed style loading.

## Project layout

- `app/src/main/java/com/atb/systemplus/SystemPlusEntry.java`
- `app/src/main/java/com/atb/systemplus/HookSettings.kt`
- `app/src/main/assets/xposed_init`
- `app/src/main/assets/module.prop`
- `app/src/main/AndroidManifest.xml`

The project now compiles against real local public Xposed artifacts found by filename under the parent directory:

- `XposedBridgeAPI-89.jar`
- `XposedBridgeAPI-82.jar`
- `libXposed-Api-101.0.1.aar`

## Hook entry

`xposed_init` points to:

`com.atb.systemplus.SystemPlusEntry`

## Build

Bootstrap Gradle wrapper (one-time), then build:

```powershell
cd D:\sysp\app\src\NEWWIRTE
pwsh -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle-wrapper.ps1
.\gradlew.bat :app:assembleDebug
```

If `gradle` is installed globally, you can also use:

```powershell
cd D:\sysp\app\src\NEWWIRTE
gradle :app:assembleDebug
```

## Quick verification

```powershell
cd D:\sysp\app\src\NEWWIRTE
.\gradlew.bat :app:testDebugUnitTest
```

The unit test currently validates token control behavior in `HookSettings.hasPrivilegeToken(...)`.

## Notes

- Build dependency mode is `compileOnly`, so Xposed APIs are not packaged into the APK.
- Runtime still expects real Xposed APIs provided by the framework on device.
- This rewrite keeps the original control model: `initZygote`, `handleLoadPackage`, grouped installers, and preference/token gating.
