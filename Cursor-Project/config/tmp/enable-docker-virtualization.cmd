@echo off
:: Run as Administrator: right-click this file -> Run as administrator
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0enable-docker-virtualization.ps1"
echo.
echo Exit code: %ERRORLEVEL%
echo Log: %~dp0docker-virt-fix.log
pause
