param(
    [switch]$IncludeConnectedAndroidTests,
    [switch]$IncludeBaselineProfile,
    [string]$ManagedDeviceName = "pixel6Api31"
)

$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

$gradleFlags = @("--no-parallel", "--max-workers=2")

function Resolve-JavaHome {
    $candidates = @(
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.9-hotspot",
        "C:\Program Files\Android\Android Studio\jbr",
        $env:JAVA_HOME
    ) | Where-Object { $_ }

    foreach ($candidate in $candidates) {
        $javaExe = Join-Path $candidate "bin\java.exe"
        if (-not (Test-Path $javaExe)) {
            continue
        }

        $versionOutput = & $javaExe -version 2>&1 | Out-String
        if ($versionOutput -match '"(?<major>\d+)') {
            $majorVersion = [int]$Matches["major"]
        } elseif ($versionOutput -match '"1\.(?<legacy>\d+)') {
            $majorVersion = [int]$Matches["legacy"]
        } else {
            continue
        }

        if ($majorVersion -ge 21) {
            return $candidate
        }
    }

    throw "未找到可用的 Java 21/JBR。请安装 Temurin 21 或 Android Studio，并确认 JAVA_HOME 未指向旧版 JDK。"
}

function Resolve-AndroidSdkRoot {
    $candidates = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    ) | Where-Object { $_ }

    foreach ($candidate in $candidates) {
        if (Test-Path (Join-Path $candidate "platform-tools\adb.exe")) {
            return $candidate
        }
    }

    throw "未找到 Android SDK。请确认 Android Studio 已安装 SDK，并且 $env:LOCALAPPDATA\Android\Sdk 可用。"
}

$resolvedJavaHome = Resolve-JavaHome
$resolvedAndroidSdkRoot = Resolve-AndroidSdkRoot

$env:JAVA_HOME = $resolvedJavaHome
$env:ANDROID_SDK_ROOT = $resolvedAndroidSdkRoot
$env:ANDROID_HOME = $resolvedAndroidSdkRoot

if ($env:Path -notlike "*$resolvedJavaHome*") {
    $env:Path = "$resolvedJavaHome\bin;$env:Path"
}
if ($env:Path -notlike "*$resolvedAndroidSdkRoot*") {
    $env:Path = "$resolvedAndroidSdkRoot\platform-tools;$env:Path"
}

function Invoke-Step {
    param(
        [string]$Label,
        [string[]]$Arguments
    )

    Write-Host ""
    Write-Host "==> $Label"
    & ".\gradlew.bat" @gradleFlags @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle step failed: $Label (exit code $LASTEXITCODE)"
    }
}

Write-Host "Running Windows CI verification from: $rootDir"
Write-Host "Using JAVA_HOME: $resolvedJavaHome"
Write-Host "Using ANDROID_SDK_ROOT: $resolvedAndroidSdkRoot"

Invoke-Step "Unit tests" @(":app:testDebugUnitTest")
Invoke-Step "Android Lint" @(":app:lintDebug")
Invoke-Step "Debug assemble" @(":app:assembleDebug")
Invoke-Step "Release assemble" @(":app:assembleRelease")

if ($IncludeConnectedAndroidTests) {
    Invoke-Step "Connected Android tests" @(":app:connectedDebugAndroidTest")
}

if ($IncludeBaselineProfile) {
    Invoke-Step "Managed-device baseline profile verification" @(":baselineprofile:${ManagedDeviceName}BenchmarkAndroidTest")
}

Write-Host ""
Write-Host "Windows CI verification completed successfully."
