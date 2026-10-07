@echo off
rem Conecta emulador y celulares USB con el backend de tu PC (puerto 8081).
rem Ejecutalo cada vez que abras el emulador o conectes el cable.
setlocal enabledelayedexpansion
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB%" (
  echo No encontre adb en: %ADB%
  echo Edita este archivo y corrige la ruta del SDK.
  pause
  exit /b 1
)
set FOUND=0
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
  if "%%b"=="device" (
    "%ADB%" -s %%a reverse tcp:8081 tcp:8081
    echo Conectado: %%a
    set FOUND=1
  )
)
if "!FOUND!"=="0" echo No hay emulador ni celular conectado. Revisa con: adb devices
pause
