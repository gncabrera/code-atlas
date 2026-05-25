@echo off
setlocal EnableExtensions

cd /d "%~dp0"

echo [run] Starting Code Atlas from %CD%

if not exist "app\code-atlas.jar" (
    echo [run] Missing app\code-atlas.jar
    exit /b 1
)
echo [run] Found app\code-atlas.jar

if not exist "data" mkdir "data"
if not exist "data\logs" mkdir "data\logs"
echo [run] Data directory: %CD%\data

if /I "%ATLAS_SKIP_UPDATE%"=="1" (
    echo [run] Skipping update check ^(ATLAS_SKIP_UPDATE=1^)
) else (
    echo [run] Checking for updates...
    call "update.bat"
)

set "JAVA_CMD=%CD%\app\runtime\bin\java.exe"
if not exist "%JAVA_CMD%" set "JAVA_CMD=java"
echo [run] Launching with Java: %JAVA_CMD%

"%JAVA_CMD%" "-Datlas.data.dir=%CD%\data" -jar "%CD%\app\code-atlas.jar"
set "EXIT_CODE=%ERRORLEVEL%"

endlocal & exit /b %EXIT_CODE%
