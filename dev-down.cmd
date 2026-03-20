@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\stop-dev-aws-environment.ps1" %*
endlocal
