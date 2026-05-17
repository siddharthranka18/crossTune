<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="120" alt="crossTune Logo"/>
</p>

<h1 align="center">crossTune</h1>

<p align="center">
  <b>A cross-platform music streaming app for Android — search, stream, import, and jam together in real time.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Language-Java-ED8B00?logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Min%20SDK-28-blue" alt="Min SDK 28"/>
  <img src="https://img.shields.io/badge/Target%20SDK-35-blue" alt="Target SDK 35"/>
  <img src="https://img.shields.io/badge/Backend-MySQL%20(Railway)-4479A1?logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Auth-Firebase-FFCA28?logo=firebase&logoColor=black" alt="Firebase"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

---

## ✨ Overview

**crossTune** is a feature-rich Android music streaming application that lets users discover, search, and stream millions of songs via the JioSaavn API — all without a subscription. It goes beyond simple playback by supporting **real-time collaborative Jam sessions** (via Socket.IO), **cross-platform playlist importing** from Apple Music and YouTube, a **behavioral telemetry engine** that tracks listening patterns, and a fully **cloud-synced MySQL backend** for persistent playlists and user analytics.

---

## 🎯 Key Features

| Feature | Description |
|---|---|
| 🔍 **Smart Search & Discover** | Real-time song search powered by JioSaavn APIs with a curated discover feed (Trending, On Repeat, Made For You) |
| 🎵 **High-Quality Streaming** | 320 kbps audio playback via Media3 ExoPlayer with background foreground-service support |
| 🔐 **Google Sign-In** | One-tap authentication using Firebase Auth + Google Sign-In |
| 🎸 **Live Jam Sessions** | Create or join a 6-digit Jam room — the host's playback syncs to all participants in real time via Socket.IO |
| 📥 **Playlist Import** | Paste an Apple Music or YouTube Music playlist URL to bulk-import all tracks |
| 📂 **Playlist Management** | Create custom playlists, like songs, and manage a downloads library — all cloud-synced to MySQL |
| 📊 **User Insights** | Profile page shows your **Top 5 Artists** computed via SQL `VIEW` + `JOIN` + `GROUP BY` on the remote database |
| 🧠 **Telemetry Engine** | Tracks skip behaviour, listening duration, and artist affinity to detect mood pivots |
| ⬇️ **Offline Downloads** | Download any song as a 320 kbps MP3 for offline playback |
| 🎨 **Accent Theming** | Five curated accent colors that dynamically tint the entire UI (sidebar, seek bar, player) |
| 〰️ **Squiggly Seek Bar** | Custom-drawn animated waveform seek bar that wiggles while music plays |
| 🐣 **Easter Egg** | Tap the Settings background 7 times to unlock Developer Mode 🤫 |

---

## 🏗️ Architecture

```
com.example.crossTune
├── LoginActivity          # Google Sign-In → Firebase Auth → upsert user to MySQL
├── MainActivity           # Sidebar navigation, bottom player bar, Media3 controller
├── SearchFragment         # Discover feed + search with multi-API fallback
├── PlayerFragment         # Full-screen player with stream resolution & seek bar
├── PlaylistsFragment      # Master-detail view with tri-state playlist cards
├── ProfileFragment        # User info, Top 5 Artists insights, account deletion
├── SettingsFragment       # Accent color picker with selection ring animation
├── SharedMusicViewModel   # Central brain: queue engine, telemetry, jam, playlists
├── MusicService           # Media3 MediaSessionService (foreground service)
├── DB                     # MySQL JDBC connector (Railway) with ACID transactions
├── Song                   # Data model (id, title, artist, album, thumbnail, stream)
├── Playlist               # Playlist data model
├── SongAdapter            # RecyclerView adapter for song lists
├── SquigglySeekBar        # Custom View: animated wavy seek bar
├── PlaylistBulkImporter   # Batch search-and-add for imported playlists
├── Apple                  # Apple Music playlist scraper (via custom REST API)
└── Youtube                # YouTube Music playlist fetcher (via custom REST API)
```

### System Diagram

```
┌─────────────┐    Firebase Auth    ┌──────────────┐
│  Android App │ ──────────────────▶ │   Firebase    │
│  (crossTune) │                    └──────────────┘
│              │    JDBC (MySQL)    ┌──────────────┐
│              │ ──────────────────▶ │   Railway DB  │
│              │                    │  (MySQL 8+)   │
│              │    REST / JSON     ┌──────────────┐
│              │ ──────────────────▶ │ JioSaavn API  │
│              │                    └──────────────┘
│              │    Socket.IO       ┌──────────────┐
│              │ ◀────────────────▶ │  Jam Relay    │
│              │                    │  (Render)     │
│              │    REST            ┌──────────────┐
│              │ ──────────────────▶ │ Apple/YT APIs │
└─────────────┘                    └──────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 11 |
| **UI Framework** | Android XML + ConstraintLayout + Material Components |
| **Media Playback** | AndroidX Media3 (ExoPlayer) + MediaSession |
| **Authentication** | Firebase Auth + Google Sign-In |
| **Database** | MySQL 8+ hosted on [Railway](https://railway.app) (JDBC connector) |
| **Networking** | OkHttp 4.12 |
| **Image Loading** | Glide 4.16 |
| **Real-time Sync** | Socket.IO Client 2.1 |
| **Cloud Services** | Firebase (Auth, Firestore), Render (Jam relay, Apple/YT APIs) |
| **Build System** | Gradle (Kotlin DSL) |

---

## 📦 Database Schema

The MySQL database (`railway`) uses the following core tables:

| Table | Purpose |
|---|---|
| `Users` | Stores user profiles (`UserID`, `name`, `email`) |
| `Playlists` | User-created playlists (`PlaylistID`, `UserID`, `name`, `createdAt`) |
| `PlaylistSongs` | Many-to-many link between playlists and songs |
| `SongCache` | Cached song metadata (`SongID`, `title`, `artist`, `album`, `durationSec`, `artworkUrl`) |

### SQL View

```sql
CREATE VIEW UserArtistStats AS
SELECT p.UserID, sc.artist, COUNT(*) AS song_count
FROM PlaylistSongs ps
  JOIN Playlists p  ON ps.PlaylistID = p.PlaylistID
  JOIN SongCache sc ON ps.SongID     = sc.SongID
GROUP BY p.UserID, sc.artist;
```

### Stored Procedure

`GetPlaylistSummary(playlistId)` — returns `song_count` and `total_seconds` for a given playlist.

### ACID Transactions

Account deletion uses a multi-step transaction (`deleteUserDataTransactional`) that removes `PlaylistSongs → Playlists → Users` atomically, with rollback on failure.

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2+) or later
- **JDK 11** or higher
- A **Google Firebase** project with Authentication enabled
- Your own `google-services.json` placed in `app/`

### Build & Run

```bash
# Clone the repository
git clone https://github.com/your-username/crossTune.git
cd crossTune

# Open in Android Studio, sync Gradle, then run on a device/emulator (API 28+)
```

### Firebase Setup

1. Create a new Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Google Sign-In** under Authentication → Sign-in method
3. Download `google-services.json` and place it in `app/`
4. Add your app's SHA-1 fingerprint to the Firebase console

### Database Setup

The app connects to a MySQL database. To set up your own:

1. Provision a MySQL 8+ instance (e.g., on [Railway](https://railway.app))
2. Create the required tables (`Users`, `Playlists`, `PlaylistSongs`, `SongCache`)
3. Run `sql/create_views.sql` to create the `UserArtistStats` view and indexes
4. Update the connection credentials in `DB.java`

---

## 📁 Project Structure

```
crossTune/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/crossTune/   # All Java source files (18 classes)
│   │   ├── res/
│   │   │   ├── layout/                   # XML layouts for activities & fragments
│   │   │   ├── drawable/                 # Icons and vector assets
│   │   │   ├── values/                   # Colors, strings, themes
│   │   │   └── mipmap-*/                 # Launcher icons (all densities)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts                  # App-level dependencies
├── sql/
│   └── create_views.sql                  # Database views and indexes
├── build.gradle.kts                      # Project-level config
├── settings.gradle.kts
└── README.md
```

---

## 🔒 Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Streaming, API calls, database access |
| `FOREGROUND_SERVICE` | Background music playback |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media-type foreground service (Android 14+) |
| `POST_NOTIFICATIONS` | Playback notification controls |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <b>Built with ❤️ by the crossTune team</b>
</p>
