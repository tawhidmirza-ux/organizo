# Organizo

Offline notes, tasks, calendar, PDF and document viewer.
Kotlin, Jetpack Compose (Material 3), MVVM, Room. Package: com.organizo.app. minSdk 26.

## Build the APK (Android Studio)
1. Install Android Studio (latest stable) from developer.android.com/studio.
2. File > Open > select this Organizo folder. Wait for "Gradle sync" to finish (first time downloads ~1 GB).
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Click "locate" in the notification. File: app/build/outputs/apk/debug/app-debug.apk

## Build the APK without Android Studio (GitHub)
1. Create a free GitHub account and a new empty repository.
2. Upload all files of this folder (keep the .github folder).
3. Repository > Actions > "Build Debug APK" > Run workflow.
4. When it finishes, open the run and download the artifact "Organizo-debug-apk" (a zip containing app-debug.apk).

## Install on your Samsung phone
Copy app-debug.apk to the phone, open it, and allow "Install unknown apps" for your file manager when asked.
