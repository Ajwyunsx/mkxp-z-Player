param(
    [ValidateSet("Debug", "Release")]
    [string] $BuildType = "Debug",
    [string] $AbiFilters = "arm64-v8a,armeabi-v7a",
    [string] $ToolchainDirs = "",
    [string] $CMakeOptions = "-DPLAYER_BUILD_LIBLCF=ON -DPLAYER_BUILD_LIBLCF_BRANCH=0.8.1",
    [switch] $CopyToEngineJniLibs,
    [switch] $OverwriteSharedDependencies
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..\..\..")).Path
$sourceDir = Join-Path $repoRoot "third_party\easyrpg-player"
$androidDir = Join-Path $sourceDir "builds\android"
$gradlew = Join-Path $androidDir "gradlew.bat"
$logDir = Join-Path $repoRoot "build-logs"
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logPath = Join-Path $logDir "easyrpg-android-$($BuildType.ToLowerInvariant())-$stamp.log"

if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "EasyRPG Android Gradle wrapper not found: $gradlew"
}

if ([string]::IsNullOrWhiteSpace($ToolchainDirs) -and $env:EASYRPG_BUILDSCRIPTS) {
    $ToolchainDirs = Join-Path $env:EASYRPG_BUILDSCRIPTS "android"
}

if ([string]::IsNullOrWhiteSpace($ToolchainDirs)) {
    throw "Set -ToolchainDirs or EASYRPG_BUILDSCRIPTS. EasyRPG Android dependencies are provided by https://github.com/EasyRPG/buildscripts."
}

if (-not (Test-Path -LiteralPath $ToolchainDirs)) {
    throw "EasyRPG Android toolchain directory does not exist: $ToolchainDirs"
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$variantProperty = if ($BuildType -eq "Debug") { "ABI_FILTERS_DEBUG" } else { "ABI_FILTERS_RELEASE" }
$gradleArgs = @(
    ":app:assemble$BuildType",
    "--console=plain",
    "-PtoolchainDirs=$ToolchainDirs",
    "-P$variantProperty=$AbiFilters",
    "-PcmakeOptions=$CMakeOptions"
)

Write-Host "EasyRPG source: $sourceDir"
Write-Host "EasyRPG Android project: $androidDir"
Write-Host "ABI filters: $AbiFilters"
Write-Host "CMake options: $CMakeOptions"
Write-Host "Log: $logPath"

Push-Location $androidDir
try {
    $previousErrorActionPreference = $ErrorActionPreference
    $nativePreferenceVariable = Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue
    if ($nativePreferenceVariable) {
        $previousNativeCommandPreference = $PSNativeCommandUseErrorActionPreference
        $PSNativeCommandUseErrorActionPreference = $false
    }
    $ErrorActionPreference = "Continue"
    & $gradlew @gradleArgs 2>&1 | Tee-Object -FilePath $logPath
    $exitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
    if ($nativePreferenceVariable) {
        $PSNativeCommandUseErrorActionPreference = $previousNativeCommandPreference
    }
    Pop-Location
}

if ($exitCode -ne 0) {
    throw "EasyRPG Android build failed with exit code $exitCode. See $logPath"
}

if (-not $CopyToEngineJniLibs) {
    Write-Host "Build finished. Use -CopyToEngineJniLibs to copy native libraries into :engine."
    exit 0
}

$mergedLibRoot = Join-Path $androidDir "app\build\intermediates\merged_native_libs"
$nativeLibs = Get-ChildItem -LiteralPath $mergedLibRoot -Recurse -Filter "*.so" -ErrorAction SilentlyContinue
if (-not $nativeLibs) {
    throw "No native libraries found under $mergedLibRoot"
}

$engineJniLibs = Join-Path $repoRoot "engine\src\main\jniLibs"
$protectedNames = @("libSDL2.so", "libc++_shared.so")

foreach ($lib in $nativeLibs) {
    $abi = Split-Path -Leaf $lib.DirectoryName
    if ($AbiFilters.Split(",") -notcontains $abi) {
        continue
    }

    $targetDir = Join-Path $engineJniLibs $abi
    $target = Join-Path $targetDir $lib.Name
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    if ((Test-Path -LiteralPath $target) -and $protectedNames -contains $lib.Name -and -not $OverwriteSharedDependencies) {
        Write-Host "Skip existing shared dependency: $target"
        continue
    }

    Copy-Item -LiteralPath $lib.FullName -Destination $target -Force
    Write-Host "Copied $($lib.Name) -> $target"
}

Write-Host "EasyRPG native libraries copied into $engineJniLibs"
