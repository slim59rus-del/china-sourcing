@echo off
rem Сборка APK на Windows. Нужны JDK 17 и Android SDK (или установленный Android Studio).
cd /d "%~dp0"
call gradle assembleDebug --no-daemon
if errorlevel 1 (
  echo.
  echo Сборка не прошла. Проверьте, что установлены JDK 17 и Android SDK, и что задан ANDROID_HOME.
  exit /b 1
)
echo.
echo APK готов: app\build\outputs\apk\debug\app-debug.apk
