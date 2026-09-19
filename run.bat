@echo off
echo ========================================================
echo   SmartOP: Hospital OPD Queue Management System
echo   Starting Core Java HTTP Server on Port 8080...
echo ========================================================

start "" http://localhost:8080/

cd /d "%~dp0backend"
java -cp "bin;lib/*" server.SmartOPServer
