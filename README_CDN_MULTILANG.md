# AndroidEw CDN Multilingual Fork

Changes:

- applicationId changed to `com.ethanthesleepyone.androidew.cdn` so it can coexist with upstream AndroidEw.
- bundled backend is the local `ew/` directory.
- settings page lets users choose:
  - local CDN asset directory
  - masterdata directory
- native backend receives those paths via JNI.
- added Traditional Chinese and Korean Android UI strings.

Recommended Android layout:

```text
/storage/emulated/0/Android/data/com.ethanthesleepyone.androidew.cdn/files/ew_data/assets/
├── Android/
│   ├── EN/<hash>/<file>
│   ├── KR/<hash>/<file>
│   └── ZH/<hash>/<file>
└── iOS/
    ├── EN/<hash>/<file>
    ├── KR/<hash>/<file>
    └── ZH/<hash>/<file>
```
