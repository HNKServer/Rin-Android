# CDN path clarification

The source-tree `ew/assets/` directory is a small runtime/submodule bundle compiled into `libew.so`. It is not the same thing as the multi-GB CDN packages such as `ZH-Android.7z`.

The AndroidEw settings field named External CDN package root is for the extracted large CDN packages. A single root is enough if it contains per-platform/per-language folders, for example:

```text
assets/
  Android/
    EN/
    KR/
    ZH/
  iOS/
    EN/
    KR/
    ZH/
```

The optional Masterdata override directory is separate and is only needed when you want to override bundled CSV masterdata.
