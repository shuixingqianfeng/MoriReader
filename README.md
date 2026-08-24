# MoriReader

MoriReader is a private, offline-first Android EPUB reader focused on Chinese
light-novel typography, accurate reading statistics, and a restrained liquid
glass interface.

## Build

Requirements: JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

MoriReader does not include books, accounts, advertising, payments, or cloud
services. Imported EPUB files remain in the app's private storage.
