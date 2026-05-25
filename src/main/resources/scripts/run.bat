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

call :load_config
if errorlevel 1 exit /b 1

echo [run] Config: port=%ATLAS_PORT%, enableAutoUpdate=%ATLAS_ENABLE_AUTO_UPDATE%

if /I "%ATLAS_SKIP_UPDATE%"=="1" (
    echo [run] Skipping update check ^(ATLAS_SKIP_UPDATE=1^)
) else if /I "%ATLAS_ENABLE_AUTO_UPDATE%"=="0" (
    echo [run] Skipping update check ^(enableAutoUpdate=false^)
) else (
    echo [run] Checking for updates...
    call "update.bat"
)

set "JAVA_CMD=%CD%\app\runtime\bin\java.exe"
if not exist "%JAVA_CMD%" set "JAVA_CMD=java"
echo [run] Launching with Java: %JAVA_CMD%

"%JAVA_CMD%" "-Datlas.data.dir=%CD%\data" "-Dserver.port=%ATLAS_PORT%" -jar "%CD%\app\code-atlas.jar"
set "EXIT_CODE=%ERRORLEVEL%"

endlocal & exit /b %EXIT_CODE%

:load_config
set "ATLAS_PORT=8088"
set "ATLAS_ENABLE_AUTO_UPDATE=1"
set "CONFIG_FILE=%CD%\data\config.yaml"

if not exist "%CONFIG_FILE%" (
    echo [run] Creating default config: %CONFIG_FILE%
    >"%CONFIG_FILE%" (
        echo port: 8088
        echo enableAutoUpdate: true
    )
)

for /f "usebackq tokens=2 delims=: " %%p in (`findstr /i /b "port:" "%CONFIG_FILE%"`) do set "ATLAS_PORT=%%p"
for /f "usebackq tokens=2 delims=: " %%p in (`findstr /i /b "enableAutoUpdate:" "%CONFIG_FILE%"`) do (
    if /I "%%p"=="false" set "ATLAS_ENABLE_AUTO_UPDATE=0"
    if /I "%%p"=="0" set "ATLAS_ENABLE_AUTO_UPDATE=0"
    if /I "%%p"=="no" set "ATLAS_ENABLE_AUTO_UPDATE=0"
)
exit /b 0
