@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\status-dev-aws-environment.ps1" %*
endlocal
