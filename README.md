# AndroidEw

A lightweight Android server application with a mobile client interface.

![AndroidEw](image.jpg)

## Overview

AndroidEw is a Kotlin-based Android application that provides a lightweight server solution with a companion mobile interface. The app runs the server as a foreground service, making it ideal for applications that need to run continuously in the background on Android devices.

This project provides a local version of **ew** that can run on Android devices. **ew** is "(a mostly functioning) server for Love Live! School idol festival 2 MIRACLE LIVE!" This app allows you to host the game server on your own Android phone or tablet, giving you full control over your game environment. The backend server logic is **written in Rust**, providing high performance and memory safety.

## Installation

To install AndroidEw, download the latest release APK from our [releases page](https://git.ethanthesleepy.one/ethanaobrien/ew-android/releases).

### Steps:

1. **Download the APK** from the releases page
2. **Enable installation from unknown sources** on your device (Settings → Security or Settings → Apps → Special app access)
3. **Open the downloaded APK** and tap "Install"
4. **Complete the setup** during first launch
5. **Grant required permissions** (notifications, storage, internet)
6. **Start the server** from the main screen

> Note: You'll need to have an emulator or test device setup for development builds. Production releases are configured for direct installation.

## Features

- **Background Service Server**: Runs as a foreground Android service with persistent notifications
- **Mobile Client Interface**: Clean Material Design UI for server control
- **Multi-language Support**: Supports English and Japanese (with localization via `values/strings.xml` and `values-ja/strings.xml`)
- **Automatic Startup**: Can be configured to start server automatically on device boot
- **Easter Mode**: Optional mode toggleable through app settings
- **Version Management**: Built-in update checking and download functionality
- **Secure File Installation**: Uses FileProvider for secure APK installation
- **Progress Notifications**: Real-time download progress updates with cancel functionality
- **Edge-to-Edge Display**: Modern Android UI with window insets handling

## Architecture

### Application Structure

```
AndroidEw/
├── app/
│   ├── src/main/
│   │   ├── java/one/ethanthesleepy/androidew/
│   │   │   ├── MainActivity.kt          # Main UI and server control
│   │   │   ├── StartupActivity.kt       # First-run setup wizard
│   │   │   ├── BackgroundService.kt     # Frontground server service
│   │   │   ├── AppBroadcastReceiver.kt  # Broadcast receiver for service control
│   │   │   ├── DownloadCancelReceiver.kt# Download cancellation handling
│   │   │   └── Utilities.kt             # Utility functions
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml    # Main screen layout
│   │   │   │   ├── settings.xml         # Settings screen layout
│   │   │   │   ├── startup_main.xml     # Startup layout
│   │   │   │   ├── install_game.xml     # Install game wizard
│   │   │   │   ├── permission_request.xml
│   │   │   │   └── setup_done.xml
│   │   │   ├── values/                  # English strings
│   │   │   └── values-ja/               # Japanese strings
│   │   ├── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── gradle/libs.versions.toml            # Version catalog
├── LICENSE                              # GNU GPL v3
└── README.md
```

### Key Components

#### BackgroundService

Runs the core server functionality as a foreground service (using native library `libew.so`).

- **Purpose**: Keeps the server running as a foreground service for reliability
- **Features**:
  - Foreground notification with blue light indicator
  - Automatic easter mode support
  - Start/stop server via native Rust implementation
  - Service persistence with `START_STICKY`

#### MainActivity

The main UI for server control and settings management.

- **Features**:
  - Toggle server start/stop
  - View server status
  - Version information display (app version and server internal version)
  - Settings page with preferences
  - Update checker
  - Easter mode toggle
  - Automatic startup preference
  - Git repository link

#### StartupActivity

First-run setup wizard for new users.

- **Setup Flow**:
  1. Permission requests (Android 13+)
  2. Download manual and options
  3. Game installation (if applicable)
  4. Completion

#### Utilities

Shared utility methods across the application.

- **Capabilities**:
  - Update version checking from remote server
  - Settings serialization with Kotlinx Serialization
  - File download with progress notification
  - APK installation with FileProvider
  - Cache management
  - Notification broadcasting

## Technology Stack

### Core Technologies

- **Language**: Kotlin (Android app), Rust (server backend)
- **Target Platform**: Android 16 (API 36)
- **Minimum Requirement**: Android 8.0+ (API 26)
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: MVVM-lite with View Binding

### Native Integration

- Uses native library via `System.loadLibrary("ew")`
- External Rust implementation for server logic
- ABI support: `arm64-v8a` only
- **Upstream Source**: The server library `libew.so` is built from [ew](https://git.ethanthesleepy.one/ethanaobrien/ew), a Rust-based backend repository. The source is cloned during build with the command:
  ```bash
  git clone --recurse-submodules https://git.ethanthesleepy.one/ethanaobrien/ew.git
  ```

## User Interface

### Main Screen

The primary interface features:

- **Server Status Display**: Shows server state (running/stopped/starting/stopping)
- **Control Button**: Toggle between start/stop server
- **Version Information**: Displays app version and server internal version
- **Settings Access**: Opens settings page for configuration
- **Download Section**: Links to additional downloads

### Settings Page

Contains configurable options:

- **Start on Boot**: Option to automatically launch server on device boot
- **Easter Mode**: Toggle special functionality
- **Update Checker**: Manual check for application updates
- **Git Repository**: Link to view source code

### Setup Wizard

New user setup flow:

1. Permission handling (Android 13+ notification permissions)
2. Download instructions
3. Game installation options
4. Completion screen

## Permissions

Required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## Data Storage

- **Settings**: Stored in `filesDir/settings.json` (JSON format via Kotlinx Serialization)
- **Temporary Files**: Stored in `filesDir/temp/` directory
- **Server Data**: Stored in `externalFilesDir/ew_data/`

## Updates and Releases

### Version Checking

The app automatically checks for updates by connecting to:
```
https://git.ethanthesleepy.one/ethanaobrien/ew-android/releases/download/latest/version.json
```

### Update Flow

1. Retrieves current installed version
2. Fetches latest version from release server
3. Compares versions
4. Downloads APK if update available
5. Installs using FileProvider for secure installation

### Release Process

Builds produce signed APKs using:
- Keystore: `lovelive.keystore`
- Signing via environment variables for security

## Localization

The app supports multiple languages:

- **English**: Primary language (default)
- **Japanese**: `values-ja/strings.xml`

### Supported Features

- All UI strings
- Notifications
- Error messages
- Setup wizard content

## Security Considerations

### FileProvider Configuration

Uses `FileProvider` for secure file sharing:
- `authorities = "${applicationId}.provider"`
- Respects `file_paths.xml` configuration
- Prevents path traversal vulnerabilities

### Native Library

The app loads a native library (`libew.so`) for server functionality, which:
- Runs in a separate process space
- Provides better performance
- Enables cross-platform compatibility
- Written in **Rust** for memory safety and high performance
- Built from upstream source at [git.ethanthesleepy.one/ethanaobrien/ew](https://git.ethanthesleepy.one/ethanaobrien/ew)

## Development

### Building the App

1. **Prerequisites**:
   - Android Studio Hedgehog or later
   - JDK 17 or later
   - Android SDK 36 or later
   - Rust toolchain with AArch64 target
   - Android NDK r29 or later

2. **Build Commands**:
   ```bash
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```

3. **Setting up for Development**:
   - Clone the upstream `ew` repository: `git clone --recurse-submodules git.ethanthesleepy.one/ethanaobrien/ew.git`
   - Install cargo-ndk: `cargo install cargo-ndk`
   - Add AArch64 target: `rustup target add aarch64-linux-android`

4. **Building Native Libraries**:
   ```bash
   cd ew
   cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs/ build --release --features library
   ```

5. **Signing Release Builds**:
   - Configure keystore in `app/build.gradle.kts`
   - Use environment variables for sensitive data:
     - `RELEASE_KEYSTORE_PASSWORD`
     - `RELEASE_KEYSTORE_ALIAS`
     - `RELEASE_KEY_PASSWORD`

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.

See the [LICENSE](LICENSE) file for full license text.

### What This Means

- ✓ You can freely use, modify, and distribute the software
- ✓ You must make your modifications available under the same license
- ✓ No warranty provided
- ✓ Source code must be provided when distributing binaries

## Credits

- **Author**: Ethan O'Brien [@ethanaobrien](https://git.ethanthesleepy.one/ethanaobrien)
- **Repository**: [Git - ethanaobrien/ew-android](https://git.ethanthesleepy.one/ethanaobrien/ew-android)
