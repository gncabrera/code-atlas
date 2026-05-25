@echo off
setlocal EnableExtensions

cd /d "%~dp0"

if not exist "app\code-atlas.jar" (
    echo Missing app\code-atlas.jar
    exit /b 1
)

if not exist "data" mkdir "data"
if not exist "data\logs" mkdir "data\logs"

if /I not "%ATLAS_SKIP_UPDATE%"=="1" (
    call "update.bat" --silent
)

set "JAVA_CMD=%CD%\app\runtime\bin\java.exe"
if not exist "%JAVA_CMD%" set "JAVA_CMD=java"

"%JAVA_CMD%" "-Datlas.data.dir=%CD%\data" -jar "%CD%\app\code-atlas.jar"
set "EXIT_CODE=%ERRORLEVEL%"

endlocal & exit /b %EXIT_CODE%
