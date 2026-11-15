# Notstore

An Android app that saves every notification your phone receives, so you can search them later and see which apps interrupt you most.

- Captures notifications in the background with a `NotificationListenerService`, even when the app is closed.
- Stores everything locally in SQLite. The app has no internet permission.
- Overview with today's hourly chart, the last 7 days, the most active apps and a weekday-by-hour heatmap.
- History with search and a date range filter, per-app views, and a cache breakdown in Settings.

Built with Kotlin and Jetpack Compose. Min SDK 26, target SDK 37.

## Build

```sh
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Use the release build to judge performance: Compose runs much slower in debug builds. The release build is signed with the debug key for local installs.

After installing, open the app and allow notification access. On Xiaomi, Redmi and POCO phones, also turn on Autostart (Settings → Autostart in the app).
