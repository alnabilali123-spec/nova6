# NovaTube

> A modern, fully-featured Android media downloader, browser and player — built with Kotlin, Jetpack Compose, Material 3, MVVM and Media3.

NovaTube is a no-compromise alternative to apps like SnapTube and Videoder. It downloads
audio and video from **1000+ sites** via [yt-dlp](https://github.com/yt-dlp/yt-dlp), runs a
real in-app web browser with multi-tab support, and provides an advanced media player with
gesture controls, Picture-in-Picture, background audio and playlists.

## Features

- 🎬 **Real downloads** — full yt-dlp binary shipped inside the APK (no JitPack, no third-party wrapper). Up to 8 parallel downloads with live progress, retry / cancel / resume.
- 🔍 **Search** — YouTube and SoundCloud via `ytsearch:` / `scsearch:` prefixes; suggestions, recent queries, kind filters.
- 🌐 **Browser** — multi-tab WebView with bookmarks, history, desktop mode, JavaScript toggle, and one-tap "send to downloader" interception.
- 🎵 **Library** — completed downloads indexed by type (audio / video), rename, share, open in player.
- 🎧 **Player** — Media3 ExoPlayer, picture-in-picture, gesture-based brightness/volume/seek, speed presets, background audio via MediaSessionService.
- 📋 **Playlists** — create, manage, queue tracks from the library.
- 📥 **Share intent** — receive URLs from any other app and jump straight to the format picker.
- 🌍 **i18n** — full English + Arabic translations with RTL.
- 🎨 **Material 3** — dynamic color on Android 12+, light/dark/system, brand red accent.

## Architecture

```
app/
├── core/                      # yt-dlp ProcessBuilder wrapper
├── data/
│   ├── entity/                # Room entities
│   ├── dao/                   # Room DAOs
│   ├── db/                    # AppDatabase
│   ├── model/                 # MediaInfo, MediaFormat, SearchResult
│   ├── prefs/                 # DataStore-backed preferences
│   └── repository/            # DownloadRepository, HistoryRepository, …
├── download/                  # CoroutineWorker + WorkManager scheduler
├── player/                    # VideoPlayerHolder, MusicPlaybackService
├── service/                   # DownloadServiceImpl
├── ui/
│   ├── components/            # Cards, badges, progress
│   ├── screens/               # home / search / browser / downloads / library / format / player / settings / music / playlists / history
│   └── theme/                 # Color, Type, Shape, Theme
├── nav/                       # Routes
├── viewmodel/                 # All VMs
├── util/                      # FileUtils, UrlUtils, OkHttpProvider, NotificationHelper, AsyncImage
└── NovaTubeApp.kt             # Application class — extracts yt-dlp, creates channels
```

## Build

The repository ships with the `yt-dlp` binary already at `app/src/main/assets/yt-dlp`,
so a clean checkout is all you need:

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Build via GitHub Actions

`.github/workflows/build.yml` is configured out of the box. Every push runs
`./gradlew assembleDebug` on Ubuntu + JDK 17 and uploads the resulting APK as an
artifact named `NovaTube-debug`.

## Tech stack

| Layer       | Library |
|-------------|---------|
| Language    | Kotlin 1.9.24 |
| UI          | Jetpack Compose 1.7.x + Material 3 1.3.x |
| Navigation  | androidx.navigation:navigation-compose |
| Database    | Room 2.6.x (KSP) |
| Prefs       | DataStore Preferences |
| Player      | AndroidX Media3 1.4.1 (ExoPlayer + MediaSessionService) |
| Network     | OkHttp 4.12 (also used by Media3's OkHttpDataSource) |
| Async       | Kotlin Coroutines 1.8 + WorkManager 2.9 |
| Engine      | yt-dlp binary (executed via ProcessBuilder) |
| Icons       | Material Icons Extended |
| Build       | Gradle 8.7 + AGP 8.5 + KSP 1.9.24-1.0.20 |

## License

MIT
