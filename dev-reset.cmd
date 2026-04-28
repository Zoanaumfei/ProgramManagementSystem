@echo off
setlocal
echo This will clear MVP data from the development platform.
echo It will preserve only INTERNAL ADMIN, Core Oryzem, and baseline templates.
echo.
set /p CONFIRM=Type RESET to continue: 
if /I not "%CONFIRM%"=="RESET" (
  echo Cancelled.
  exit /b 1
)
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\clear-mvp-platform-data-ecs.ps1" -ConfirmToken RESET_MVP_PLATFORM_DATA
endlocal
