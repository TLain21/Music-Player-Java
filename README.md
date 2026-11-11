# 🎵 Aurora Music Player (Java 21 Desktop Audio Player)

A modern, high-performance desktop music player built with **Java 21**, **FlatLaf**, and **Java Sound SPI**. Designed specifically to play multiple high-fidelity audio formats including **.wav**, **.mp3**, **.flac**, **.ogg**, and more, with real-time spectrum visualization and full metadata support.

---

## 🚀 Key Features

- **Multi-Format Audio Engine (Java Sound SPI)**:
  - **WAV** (`.wav`): High-fidelity 16-bit / 24-bit / 32-bit uncompressed PCM.
  - **MP3** (`.mp3`): MPEG Audio Layer III (CBR and VBR up to 320 kbps) via `mp3spi` & `jlayer`.
  - **FLAC** (`.flac`): Studio-quality Lossless Audio via `jflac-codec`.
  - **OGG Vorbis** (`.ogg`): Ogg Vorbis streams via `vorbisspi` & `jorbis`.
  - **AIFF / AU** (`.aif`, `.aiff`, `.au`): Standard uncompressed formats.
- **Real-Time 36-Band Audio Spectrum Visualizer**:
  - Live frequency spectrum analyzer rendered at 50 FPS with smooth decay and peak-hold gravity physics.
  - Glowing modern neon violet to cyan gradient bars.
  - Idle breathing pulse animation when paused or stopped.
- **Sleek Modern UI (FlatLaf Dark Theme)**:
  - Spotify/Tidal-inspired desktop interface with rounded corners, custom scrollbars, and high-DPI scaling.
  - Persistent bottom playback control bar with miniature album art and title.
- **Rich Metadata & Cover Art**:
  - Automatic extraction of ID3v1/ID3v2 tags, FLAC Vorbis comments, artist, album, year, genre, and bitrate.
  - Embedded cover art rendering with smooth rounded corners and subtle drop shadows.
  - Dynamic geometric vinyl art generator for tracks without embedded images.
- **Playlist & Library Management**:
  - **Drag & Drop**: Drag music files or entire folders directly from Windows Explorer into the player.
  - **Recursive Folder Scanning**: Easily import an entire music library with "Add Folder".
  - **Instant Live Search**: Filter tracks on-the-fly by title, artist, or album.
  - Right-click context menus: "Play Now", "Open File Location in Explorer", "Remove Track".
- **Advanced Playback Controls**:
  - Play, Pause, Resume, Next, Previous.
  - True non-repeating Shuffle mode (`🔀`).
  - Repeat modes: Off, Repeat All (`🔁`), Repeat Track (`🔂`).
  - Interactive Scrubbing / Seekbar with live tooltip hover.
  - Master volume slider with Mute toggle and logarithmic decibel scaling.

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
| :--- | :--- |
| <kbd>Space</kbd> | Play / Pause |
| <kbd>Ctrl</kbd> + <kbd>→</kbd> | Next Track |
| <kbd>Ctrl</kbd> + <kbd>←</kbd> | Previous Track |
| <kbd>→</kbd> | Fast-forward 5 seconds |
| <kbd>←</kbd> | Rewind 5 seconds |
| <kbd>↑</kbd> | Volume Up (+5%) |
| <kbd>↓</kbd> | Volume Down (-5%) |

---

## 📂 Project Structure

```
project_MP3_WAV/
├── aurora-player.jar          # Packaged runnable desktop executable
├── build.bat                  # Script to compile and build the JAR
├── run.bat                    # 1-Click launcher for Windows
├── run.ps1                    # PowerShell launcher
├── README.md                  # Documentation
├── lib/                       # SPI & UI JAR libraries
│   ├── flatlaf-3.5.4.jar
│   ├── jaudiotagger-3.0.1.jar
│   ├── jflac-codec-1.5.2.jar
│   ├── jlayer-1.0.1.4.jar
│   ├── jorbis-0.0.17.4.jar
│   ├── mp3spi-1.9.5.4.jar
│   ├── tritonus-share-0.3.7.4.jar
│   └── vorbisspi-1.0.3.3.jar
├── samples/                   # Pre-bundled test audio tracks
│   ├── sample_chords.wav
│   └── sample_electronic_beat.wav
└── src/main/java/com/aurora/
    ├── Main.java              # App entrypoint & UI look-and-feel setup
    ├── audio/
    │   ├── AudioPlayer.java         # Streaming decoder, PCM playback & volume engine
    │   ├── AudioPlayerListener.java # Event callbacks for UI and visualizer
    │   ├── PlaybackState.java       # STOPPED, PLAYING, PAUSED
    │   └── RepeatMode.java          # OFF, ALL, ONE
    ├── model/
    │   ├── Track.java               # Audio track model with formatted metadata
    │   ├── Playlist.java            # Playlist manager with shuffle queue
    │   └── TrackMetadataReader.java # Jaudiotagger & Java Sound fallback extractor
    └── ui/
        ├── MainWindow.java          # Main window layout, toolbar, shortcuts & dialogs
        ├── NowPlayingPanel.java     # Artwork, title, format badges & embedded visualizer
        ├── PlaylistPanel.java       # Track table, drag & drop, context menu & live search
        ├── PlayerControlBar.java    # Bottom persistent playback controls & volume
        ├── VisualizerPanel.java     # Real-time animated 36-band spectrum visualizer
        └── AlbumArtUtil.java        # Smooth image scaling, rounded borders & placeholder art
```

---

## 🛠️ How to Run

### Option 1: Double Click (Fastest)
Double-click `run.bat` in File Explorer.

### Option 2: PowerShell
Run:
```powershell
.\run.ps1
```

### Option 3: Command Line
```cmd
java -jar aurora-player.jar
```
*(Or specify the Microsoft OpenJDK path if not globally on PATH: `& "C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot\bin\java.exe" -jar aurora-player.jar`)*

---

## 🔨 How to Rebuild
To rebuild after modifying any source code, simply run:
```cmd
build.bat
```
This compiles all sources against `lib/*` and updates `aurora-player.jar`.
