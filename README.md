# SweatLock

Android app (Kotlin, Jetpack Compose, Min SDK 26) that prevents doomscrolling by locking distracting apps until the user completes a verified pushup or squat workout counted live on-device via Google ML Kit Pose Detection.

## How to Build via GitHub Actions (No Android Studio Needed)
1. Create a new repository on GitHub (e.g. `SweatLock`).
2. Extract this ZIP archive locally (make sure hidden files are visible so `.github` is included).
3. Drag and drop all extracted files and folders into your GitHub repository (`Add file` -> `Upload files`) and commit to `main`.
4. Open the **Actions** tab in your GitHub repository, wait for **Build SweatLock APK** to finish, and download **SweatLock-debug-apk** from **Artifacts**.
5. Install `app-debug.apk` on your Android device (Android 8.0+). On Android 13+, go to **Settings -> Apps -> SweatLock -> (⋮) -> Allow restricted settings** before toggling Accessibility Service ON.
