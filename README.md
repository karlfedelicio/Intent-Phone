# Intent Phone (Native Android Launcher)

> "Before using my phone, I should know why I am using it."

Native Android launcher written in Kotlin & Jetpack Compose (Material 3).
Targets Android 16 (API 36) down to Android 8.0 (API 26).

### Quick Build
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Set as Default
Settings > Apps > Default apps > Home app > Intent Phone.