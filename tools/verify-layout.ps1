$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$requiredFiles = @(
    'settings.gradle',
    'build.gradle',
    'gradle.properties',
    'app/build.gradle',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/assets/xposed_init',
    'app/src/main/assets/module.prop',
    'app/src/main/java/com/atb/systemplus/SystemPlusEntry.java',
    'app/src/main/java/com/atb/systemplus/HookSettings.kt'
)

$missing = @()
foreach ($relativePath in $requiredFiles) {
    $fullPath = Join-Path $root $relativePath
    if (-not (Test-Path $fullPath)) {
        $missing += $relativePath
    }
}

if ($missing.Count -gt 0) {
    Write-Output 'VERIFY_FAIL: Missing files:'
    $missing | ForEach-Object { Write-Output " - $_" }
    exit 1
}

$entry = Get-Content (Join-Path $root 'app/src/main/assets/xposed_init') -Raw
$entry = $entry.Trim()
if ($entry -ne 'com.atb.systemplus.SystemPlusEntry') {
    Write-Output "VERIFY_FAIL: xposed_init entry mismatch: $entry"
    exit 1
}

$depsRoot = Split-Path -Parent $root
$depNames = @(
    'XposedBridgeAPI-89.jar',
    'XposedBridgeAPI-82.jar',
    'libXposed-Api-101.0.1.aar'
)

$missingDeps = @()
foreach ($dep in $depNames) {
    $found = Get-ChildItem -Path $depsRoot -Recurse -File -Filter $dep -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $found) {
        $missingDeps += $dep
    }
}

if ($missingDeps.Count -gt 0) {
    Write-Output 'VERIFY_FAIL: Missing dependency artifacts:'
    $missingDeps | ForEach-Object { Write-Output " - $_" }
    exit 1
}

Write-Output 'VERIFY_OK: project layout, xposed_init, and local dependency artifacts are valid.'
