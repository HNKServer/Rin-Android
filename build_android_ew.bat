@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "MODE=%~1"
if "%MODE%"=="" set "MODE=debug"
if /I "%MODE%"=="clean" goto :clean
if /I "%MODE%"=="debug" set "GRADLE_TASK=assembleDebug"
if /I "%MODE%"=="release" set "GRADLE_TASK=assembleRelease"
if "%GRADLE_TASK%"=="" (
  echo Usage: build_android_ew.bat [debug^|release^|clean]
  exit /b 2
)

echo [AndroidEw] Mode: %MODE%

where cargo >nul 2>nul
if errorlevel 1 (
  echo ERROR: cargo was not found. Install Rust first: https://rustup.rs/
  exit /b 1
)
where rustup >nul 2>nul
if errorlevel 1 (
  echo ERROR: rustup was not found. Install Rust first: https://rustup.rs/
  exit /b 1
)
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java/JDK was not found. Install JDK 17 or Android Studio Embedded JDK.
  exit /b 1
)
where cargo-ndk >nul 2>nul
if errorlevel 1 (
  echo cargo-ndk was not found. Installing it now...
  cargo install cargo-ndk
  if errorlevel 1 exit /b %errorlevel%
)

rustup target add aarch64-linux-android
if errorlevel 1 exit /b %errorlevel%

if "%ANDROID_NDK_HOME%"=="" (
  if not "%ANDROID_NDK_ROOT%"=="" set "ANDROID_NDK_HOME=%ANDROID_NDK_ROOT%"
)
if "%ANDROID_NDK_HOME%"=="" (
  for /f "usebackq delims=" %%I in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$c=@(); if($env:ANDROID_HOME){$c += Join-Path $env:ANDROID_HOME 'ndk'}; if($env:ANDROID_SDK_ROOT){$c += Join-Path $env:ANDROID_SDK_ROOT 'ndk'}; $c += Join-Path $env:LOCALAPPDATA 'Android\Sdk\ndk'; $dirs=@(); foreach($p in $c){ if(Test-Path $p){ $dirs += Get-ChildItem $p -Directory | Sort-Object Name -Descending } }; if($dirs.Count -gt 0){ $dirs[0].FullName }"`) do set "ANDROID_NDK_HOME=%%I"
)
if "%ANDROID_NDK_HOME%"=="" (
  echo ERROR: Android NDK was not found.
  echo Install NDK in Android Studio SDK Manager, or set ANDROID_NDK_HOME.
  exit /b 1
)
set "ANDROID_NDK_ROOT=%ANDROID_NDK_HOME%"
echo Using ANDROID_NDK_HOME=%ANDROID_NDK_HOME%

if /I "%MODE%"=="release" (
  if not exist "app\lovelive.keystore" (
    echo ERROR: Release build needs app\lovelive.keystore.
    echo Generate one with:
    echo   keytool -genkeypair -v -keystore app\lovelive.keystore -alias androidewcdn -keyalg RSA -keysize 2048 -validity 10000
    exit /b 1
  )
  if "%RELEASE_KEYSTORE_PASSWORD%"=="" (
    echo ERROR: Set RELEASE_KEYSTORE_PASSWORD, RELEASE_KEYSTORE_ALIAS, RELEASE_KEY_PASSWORD before release build.
    exit /b 1
  )
  if "%RELEASE_KEYSTORE_ALIAS%"=="" (
    echo ERROR: Set RELEASE_KEYSTORE_ALIAS before release build.
    exit /b 1
  )
  if "%RELEASE_KEY_PASSWORD%"=="" (
    echo ERROR: Set RELEASE_KEY_PASSWORD before release build.
    exit /b 1
  )
)

echo [1/3] Enabling cdylib output in ew\Cargo.toml...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='ew\Cargo.toml'; $s=[IO.File]::ReadAllText($p); $s=$s -replace '(?m)^#\[lib\]','[lib]' -replace '(?m)^#crate-type','crate-type' -replace '(?m)^#required-features','required-features'; [IO.File]::WriteAllText($p,$s,(New-Object Text.UTF8Encoding($false)))"
if errorlevel 1 exit /b %errorlevel%


if not exist "ew\webui\index.html" (
  echo [AndroidEw] ew webui submodule is missing; creating a minimal placeholder ew\webui\index.html for source-zip builds.
  if not exist "ew\webui" mkdir "ew\webui"
  >"ew\webui\index.html" echo ^<!doctype html^>
  >>"ew\webui\index.html" echo ^<html^>^<head^>^<meta charset="utf-8"^>^<title^>ew WebUI placeholder^</title^>^</head^>^<body^>ew WebUI placeholder^</body^>^</html^>
)

echo [2/3] Building Rust native library libew.so...
pushd ew
cargo ndk -t arm64-v8a -o ..\app\src\main\jniLibs build --release --features library
if errorlevel 1 exit /b %errorlevel%
popd
if not exist "app\src\main\jniLibs\arm64-v8a\libew.so" (
  echo ERROR: libew.so was not created.
  exit /b 1
)

echo [3/3] Building Android APK with Gradle: %GRADLE_TASK%...
call gradlew.bat %GRADLE_TASK%
if errorlevel 1 exit /b %errorlevel%

echo.
echo Build finished.
if /I "%MODE%"=="debug" (
  echo Output: %CD%\app\build\outputs\apk\debug\app-debug.apk
) else (
  echo Output: %CD%\app\build\outputs\apk\release\app-release.apk
)
exit /b 0

:clean
echo Cleaning AndroidEw project...
if exist gradlew.bat call gradlew.bat clean
if exist ew\target rmdir /s /q ew\target
if exist app\src\main\jniLibs rmdir /s /q app\src\main\jniLibs
echo Clean finished.
exit /b 0
