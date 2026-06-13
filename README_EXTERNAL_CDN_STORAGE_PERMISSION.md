# External CDN package root and Android storage permission

`ew/assets` in the source tree is only the small built-in runtime asset submodule. It is not the same thing as the multi-GB CDN packages such as `ZH-Android.7z`.

When you choose an external CDN package root such as:

```text
/storage/emulated/0/LoveLive/ZH-Android
```

AndroidEw passes the raw filesystem path to the Rust `ew` server. On Android 11 and above, raw access to arbitrary files under `/storage/emulated/0` is blocked by scoped storage unless the app has **All files access** (`MANAGE_EXTERNAL_STORAGE`). SAF folder selection alone grants URI access to Android/Kotlin code, but not raw path access to native Rust `std::fs::read()`.

This build therefore declares `MANAGE_EXTERNAL_STORAGE` and opens the app-specific All files access settings when an external CDN path is selected. Enable the permission, then return to AndroidEw and start the server again.

If you do not want to grant All files access, put the extracted CDN packages under AndroidEw's app-specific external directory instead:

```text
/storage/emulated/0/Android/data/com.ethanthesleepyone.androidew.cdn/files/ew_data/assets/
```

The app can read that directory by raw path without All files access.

Supported roots include both of these forms:

```text
<selected-root>/Android/ZH/<hash>/<file>
<selected-root>/ZH-Android/<hash>/<file>
<selected-root>/<hash>/<file>
```

The last form lets users select the exact extracted package directory itself, for example `/storage/emulated/0/LoveLive/ZH-Android/ZH-Android`.
