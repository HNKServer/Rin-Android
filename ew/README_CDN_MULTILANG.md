# ew CDN + multilingual asset support

This fork adds local CDN serving for all SIF2 asset package layouts used by JP and GL clients.

## CLI

```bash
./ew --asset-path /path/to/assets --masterdata /path/to/masterdata
```

Supported asset layouts include:

```text
assets/Android/<hash>/<file>              # JP Android
assets/iOS/<hash>/<file>                  # JP iOS
assets/Android/EN/<hash>/<file>           # GL English Android
assets/Android/KR/<hash>/<file>           # GL Korean Android
assets/Android/ZH/<hash>/<file>           # GL Traditional Chinese Android archive layout
assets/Android/ZH-CHT/<hash>/<file>       # alias accepted by client requests
assets/iOS/EN/<hash>/<file>
assets/iOS/KR/<hash>/<file>
assets/iOS/ZH/<hash>/<file>
assets/ZH-Android/<hash>/<file>           # alternate extracted archive layout
assets/ZH/iOS/<hash>/<file>               # alternate extracted archive layout
```

`ZH-CHT`, `zh-Hant`, `zh-TW`, `zh-HK`, and `ZH` are treated as Traditional Chinese aliases. `KR`, `ko`, and `ko-KR` are Korean aliases.

Masterdata lookup prefers `csv-zh-cht/` and `csv-kr/` when present, and falls back to `csv-en/` for missing tables.
