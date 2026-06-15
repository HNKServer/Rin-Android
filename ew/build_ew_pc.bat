@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo [ew] Building PC/desktop ew server...
where cargo >nul 2>nul
if errorlevel 1 (
  echo ERROR: cargo was not found. Install Rust first: https://rustup.rs/
  exit /b 1
)

cargo --version

if not exist "webui\index.html" (
  echo [ew] webui submodule is missing; creating a minimal placeholder webui\index.html for source-zip builds.
  if not exist "webui" mkdir "webui"
  >"webui\index.html" echo ^<!doctype html^>
  >>"webui\index.html" echo ^<html^>^<head^>^<meta charset="utf-8"^>^<title^>ew WebUI placeholder^</title^>^</head^>^<body^>ew WebUI placeholder^</body^>^</html^>
)

cargo build --release
if errorlevel 1 exit /b %errorlevel%

echo.
echo Build finished.
echo Output:
echo   %CD%\target\release\ew.exe
echo.
echo Example run:
echo   target\release\ew.exe --port 8080 --path .\ew_data --asset-path D:\sif2_assets
