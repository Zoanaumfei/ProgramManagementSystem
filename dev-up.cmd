@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\start-dev-aws-environment.ps1" %*
endlocal