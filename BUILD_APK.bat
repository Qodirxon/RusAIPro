@echo off
setlocal
if not exist gradlew.bat (
  echo Gradle wrapper is missing. Open this project in Android Studio and use Gradle Sync first.
  exit /b 1
)
call gradlew.bat :app:assembleDebug
if errorlevel 1 exit /b %errorlevel%
echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
