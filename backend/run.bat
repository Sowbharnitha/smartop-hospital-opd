@echo off
echo ===================================================
echo Starting SmartOP Server (Port 8080)
echo ===================================================

cd /d "%~dp0"
java -cp "bin;lib/*" server.SmartOPServer
