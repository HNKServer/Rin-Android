# v10 JP-safe cumulative patch

This patch supersedes v9.

Included:
- JP/GL asset hash fix from the previous cumulative patches.
- Traditional Chinese csv-zh-cht translation cleanup / glossary sync from v7/v8b.
- WebUI card database multilingual search from v8b.
- WebUI local card-thumbnail hit fix, while preserving the original online 303 fallback.
- JP local bundled asset lookup fix for /Android/<hash>/<file> when assets are bundled under JP/<hash>/<file>.

Changed from v9:
- Removed the /api/start aoharu-asset-version bypass idea.  The original header-based WebUI fallback assumption is preserved.
- Added JP-safe masterdata behavior: /api/masterdata/.../JP returns an empty object so the original JP client falls back to its own JP data instead of using server-side translated/generated masterdata.
- Added JP-safe assetLists behavior: /api/assetLists/.../JP returns an empty object, avoiding server-side asset list injection for official JP client + online CDN mode.
- EN/KR/ZH-CHT masterdata and asset lists remain available for multilingual/GL usage.

Apply, rebuild libew.so/APK, and test JP without stacking v9.
