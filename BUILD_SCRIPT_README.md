# Build scripts for ew / AndroidEw CDN multi-language ZH-CHT MT builds

## PC ew server

Windows:

```bat
cd ew
build_ew_pc.bat
```

Linux/macOS:

```bash
cd ew
chmod +x build_ew_pc.sh
./build_ew_pc.sh
```

Output:

- Windows: `target\release\ew.exe`
- Linux/macOS: `target/release/ew`

Example run:

```bash
./target/release/ew --port 8080 --path ./ew_data --asset-path /path/to/sif2_assets
```

`--masterdata` is optional. If omitted, this build uses the built-in machine-translated `csv-zh-cht` masterdata.

## AndroidEw APK

Requirements:

- JDK 17
- Android Studio / Android SDK
- Android SDK Platform 36
- Android NDK
- Rust stable

Windows debug build:

```bat
cd ew-android
build_android_ew.bat debug
```

Linux/macOS debug build:

```bash
cd ew-android
chmod +x build_android_ew.sh
./build_android_ew.sh debug
```

The script will:

1. enable `[lib]` cdylib output in `ew/Cargo.toml`,
2. install/use `cargo-ndk`,
3. build `app/src/main/jniLibs/arm64-v8a/libew.so`,
4. run Gradle to build the APK.

Debug output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release build needs `app/lovelive.keystore` and these environment variables:

```text
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEYSTORE_ALIAS
RELEASE_KEY_PASSWORD
```

Generate a release keystore:

```bash
keytool -genkeypair -v -keystore app/lovelive.keystore -alias androidewcdn -keyalg RSA -keysize 2048 -validity 10000
```

Then run:

```bash
./build_android_ew.sh release
```

or on Windows:

```bat
build_android_ew.bat release
```

Clean:

```bash
./build_android_ew.sh clean
```

or:

```bat
build_android_ew.bat clean
```
