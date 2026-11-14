@echo off
setlocal
set GV=8.9
set GH=%USERPROFILE%\.gradle\joymart-distributions\gradle-%GV%
if not exist "%GH%\bin\gradle.bat" (
  if not exist "%USERPROFILE%\.gradle\joymart-distributions" mkdir "%USERPROFILE%\.gradle\joymart-distributions"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://services.gradle.org/distributions/gradle-%GV%-bin.zip'; $z=Join-Path $env:USERPROFILE '.gradle\joymart-distributions\gradle-%GV%-bin.zip'; Invoke-WebRequest $u -OutFile $z; Expand-Archive -Force $z (Join-Path $env:USERPROFILE '.gradle\joymart-distributions')" || exit /b 1
)
call "%GH%\bin\gradle.bat" -p "%~dp0" %*
