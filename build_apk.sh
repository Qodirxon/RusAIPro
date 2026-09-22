#!/usr/bin/env bash
set -e
if [ ! -f ./gradlew ]; then
  echo "Gradle wrapper is missing. Open this project in Android Studio and sync Gradle first."
  exit 1
fi
chmod +x ./gradlew
./gradlew :app:assembleDebug
echo
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
