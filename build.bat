@echo off
echo ========================================================
echo   SmartOP: Hospital OPD Queue Management System
echo   Building Core Java Backend...
echo ========================================================

cd /d "%~dp0backend"
call build.bat
cd /d "%~dp0"

echo.
echo Build complete. Run run.bat to launch the application.
