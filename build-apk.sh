#!/usr/bin/env bash
# Сборка APK на своей машине. Нужны JDK 17 и Android SDK (или установленный Android Studio).
set -e
cd "$(dirname "$0")"
echo "Gradle: $(gradle --version 2>/dev/null | head -n 3 || echo 'не найден')"
gradle assembleDebug --no-daemon
echo
echo "APK готов: app/build/outputs/apk/debug/app-debug.apk"
