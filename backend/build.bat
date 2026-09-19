@echo off
echo ===================================================
echo Compiling SmartOP Backend (Core Java + JDBC)
echo ===================================================

cd /d "%~dp0"
if not exist "bin" mkdir "bin"

echo Compiling Java source files...
javac -encoding UTF-8 -cp "lib/*" -d bin src/config/*.java src/db/*.java src/util/*.java src/model/*.java src/dao/*.java src/service/*.java src/handler/*.java src/server/*.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully into backend\bin!
) else (
    echo [ERROR] Compilation failed! Check the error output above.
    exit /b %ERRORLEVEL%
)
