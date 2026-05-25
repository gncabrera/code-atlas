@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "SILENT=0"
if /I "%~1"=="--silent" set "SILENT=1"

if not exist "app\code-atlas.jar" exit /b 0

set "REPO=%ATLAS_GITHUB_REPO%"
if not defined REPO (
    if exist "app\repo.txt" (
        set /p REPO=<"app\repo.txt"
    )
)
if not defined REPO set "REPO=gncabrera/code-atlas"

set "LOCAL_VERSION="
if exist "app\version.txt" set /p LOCAL_VERSION=<"app\version.txt"

set "WORK_DIR=%CD%\.update-tmp"
if exist "%WORK_DIR%" rmdir /s /q "%WORK_DIR%"
mkdir "%WORK_DIR%" >nul 2>&1
if errorlevel 1 exit /b 1

powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri 'https://api.github.com/repos/%REPO%/releases/latest' -Headers @{ 'User-Agent'='code-atlas-updater' } -OutFile '%WORK_DIR%\release.json'" >nul 2>&1
if errorlevel 1 goto :cleanup_ok

set "LINE_COUNT=0"
set "REMOTE_VERSION="
set "ASSET_URL="
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "$release = Get-Content -Raw '%WORK_DIR%\release.json' | ConvertFrom-Json; $assetNames = @('code-atlas-update-windows.zip','code-atlas-update.zip'); $asset = $null; foreach ($assetName in $assetNames) { $asset = $release.assets | Where-Object { $_.name -eq $assetName } | Select-Object -First 1; if ($asset) { break } }; if (-not $asset) { exit 3 }; Write-Output $release.tag_name; Write-Output $asset.browser_download_url"`) do (
    set /a LINE_COUNT+=1
    if !LINE_COUNT!==1 set "REMOTE_VERSION=%%i"
    if !LINE_COUNT!==2 set "ASSET_URL=%%i"
)

if not defined REMOTE_VERSION goto :cleanup_ok
if not defined ASSET_URL goto :cleanup_ok

if /I "%LOCAL_VERSION%"=="%REMOTE_VERSION%" goto :cleanup_ok

if "%SILENT%"=="0" echo Updating from %LOCAL_VERSION% to %REMOTE_VERSION%

powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '%ASSET_URL%' -Headers @{ 'User-Agent'='code-atlas-updater' } -OutFile '%WORK_DIR%\update.zip'" >nul 2>&1
if errorlevel 1 goto :cleanup_fail

powershell -NoProfile -Command "Expand-Archive -Path '%WORK_DIR%\update.zip' -DestinationPath '%WORK_DIR%\extracted' -Force" >nul 2>&1
if errorlevel 1 goto :cleanup_fail

set "NEW_APP_DIR="
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "$root = '%WORK_DIR%\extracted'; $candidate = Join-Path $root 'app'; if (Test-Path (Join-Path $candidate 'code-atlas.jar')) { Write-Output $candidate; exit 0 }; $nested = Get-ChildItem -Path $root -Directory | ForEach-Object { Join-Path $_.FullName 'app' } | Where-Object { Test-Path (Join-Path $_ 'code-atlas.jar') } | Select-Object -First 1; if ($nested) { Write-Output $nested; exit 0 }; exit 4"`) do (
    set "NEW_APP_DIR=%%i"
)

if not defined NEW_APP_DIR goto :cleanup_fail

if exist "app_old" rmdir /s /q "app_old"
if exist "%WORK_DIR%\runtime_backup" rmdir /s /q "%WORK_DIR%\runtime_backup"

if exist "app\runtime" (
    move "app\runtime" "%WORK_DIR%\runtime_backup" >nul
    if errorlevel 1 goto :cleanup_fail
)

rename "app" "app_old"
if errorlevel 1 goto :rollback_runtime

move "%NEW_APP_DIR%" "app" >nul
if errorlevel 1 goto :rollback_app

if exist "%WORK_DIR%\runtime_backup" (
    move "%WORK_DIR%\runtime_backup" "app\runtime" >nul
    if errorlevel 1 goto :rollback_app
)

if not exist "app\repo.txt" (
    if exist "app_old\repo.txt" copy /y "app_old\repo.txt" "app\repo.txt" >nul
)

if exist "app_old" rmdir /s /q "app_old"
goto :cleanup_ok

:rollback_app
if exist "app" rmdir /s /q "app"
if exist "app_old" rename "app_old" "app"
goto :cleanup_fail

:rollback_runtime
if exist "%WORK_DIR%\runtime_backup" move "%WORK_DIR%\runtime_backup" "app\runtime" >nul
goto :cleanup_fail

:cleanup_fail
if "%SILENT%"=="0" echo Update failed; keeping current installation.
set "EXIT_CODE=1"
goto :cleanup

:cleanup_ok
set "EXIT_CODE=0"

:cleanup
if exist "%WORK_DIR%" rmdir /s /q "%WORK_DIR%"
endlocal & exit /b %EXIT_CODE%
