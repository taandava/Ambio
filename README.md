# Ambio Mix — All-in-One Ambient Audio Mixer

A streamlined, modern ambient soundscape mixer engineered with Jetpack Compose and AndroidX Media3. Features multichannel simultaneous playback, independent track faders, repeat timing, pitch/speed modulation, sleep timer with smooth fade out, background playback, and WhatsApp-style emojis.

---

## 🌟 Key Features

- **Clean Layered Architecture**: Organized domain data models, repository persistence, background media engine, and Jetpack Compose UI.
- **Pure Text Emoji UI**: Native WhatsApp-style emojis (🎵, ⏱️, 📁, 🎨, 🎚️, 🔊, 🔇, ➕, 🔄, 🗑️, ▶️, ⏹️, 🎲, 🔍, ✖️, ✔️, ♾️) providing clean, responsive visuals.
- **Project-Aware WhatsApp-Style Emoji Launcher Icon (`🎧`)**: Resolution-independent vector and adaptive icon (`ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_monochrome.xml`) styled after the 3D WhatsApp Headphone emoji (`🎧`) with complete density assets across mdpi, hdpi, xhdpi, xxhdpi, and xxxhdpi.
- **10 Simultaneous Audio Channels**: Blend up to 10 tracks simultaneously from your device's audio library.
- **Sound Management & Import**:
  - Automatically scans device audio recordings and music via MediaStore.
  - Native system document picker integration with persistable read permissions.
- **Channel Customization & Fine-Tuning**:
  - **Custom Channel Label**: Custom title per channel.
  - **Repeat Interval (0–120s)**: Continuous loop (`♾️`) or periodic playback interval.
  - **Fuzzy Repeat (🎲 Dice)**: Randomizes timing (±30%) for natural organic variation.
  - **Pitch Modulation (0.50x–1.50x)**: Real-time frequency pitch shift.
  - **Playback Speed (0.50x–1.50x)**: Tempo control without altering pitch.
  - **Instant Mute/Solo Toggle**: Tap channel thumbnail to silence or resume immediately.
- **Master Controls**:
  - Global master volume slider preserving channel balance.
  - One-tap level reset.
  - Clear mix and multi-preset save/load management.
- **Sleep Timer ("Zzz")**:
  - Quick timer presets (15m, 30m, 45m, 60m, 90m, 120m) with live top bar countdown.
  - **Smooth Fade Out**: Automatically eases volume down over the last 15 seconds before turning off.
- **Theme & Appearance Customization**:
  - Follow System, Pure Light, and Pure Dark theme modes with accessible contrast.
- **Uninterrupted Background Playback**:
  - AndroidX Media3 Foreground Service (`PlaybackService`).
  - Notification controls with live channel status.

---

## 🛠️ Project Structure

```
Ambio/
├── build-apk.bat                                  # One-click Windows build script
├── build.gradle.kts                               # Root build script
├── settings.gradle.kts                            # Project settings
├── gradle.properties                              # JVM & AndroidX build options
├── release.jks                                    # Pre-configured release signing keystore
├── README.md                                      # Documentation & usage guide
├── app/
│   ├── build.gradle.kts                           # App build & signing configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                # Manifest with service & adaptive icon
│       │   ├── java/com/taandava/ambio/
│       │   │   ├── MainActivity.kt                # Application entrypoint & service binding
│       │   │   ├── data/
│       │   │   │   ├── model/                     # SoundModels & ThemeMode
│       │   │   │   └── repository/                # MixStore & DeviceAudioScanner
│       │   │   ├── service/                       # PlaybackService (Media3 + Sleep Timer)
│       │   │   └── ui/                            # Components, Dialogs, Screens & Theme
│       │   └── res/
│       │       ├── drawable/                      # Vector foreground, background & monochrome
│       │       ├── mipmap-*/                      # Adaptive and density launcher icons (🎧)
│       │       └── values/                        # Strings and transparent system bar styles
│       └── test/                                  # Comprehensive unit test suite
```

---

## 🚀 Building the Signed Release APK

The project is pre-configured with a 2048-bit release keystore (`release.jks`) supporting Android V1, V2, V3, and V4 signing schemes.

### Quick Build (Windows):
```cmd
build-apk.bat
```

### Manual Gradle Build:
```bash
./gradlew assembleRelease
```

The signed release APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 Running Automated Tests

```bash
./gradlew test
```
