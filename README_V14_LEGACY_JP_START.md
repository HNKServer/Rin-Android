# v14 legacy-JP startup cumulative patch

Base:
- v11b supported-only cumulative patch.

Rationale:
- The official AndroidEw release APK was built before the later ew masterdata /
  assetLists work.  Public commit history shows masterdata/file-list work was
  added after the April release window.
- The old 2026-02-era ew startup path selected asset hash only from
  asset_version + platform.
- For official JP client + online CDN mode, keep JP as close to official
  AndroidEw behavior as possible while preserving multilingual support for GL/ZH.

New changes from v11b:
1. Register /api/start and /api/start/assetHash outside the WebUI fallback
   middleware.  This avoids the later modular middleware changing startup POST
   behavior.  Other /api routes still use the original modular fallback logic.
2. Make start.rs explicitly use legacy assetVersion+platform hash selection for
   JP.  Language-specific hash selection remains available for GL/ZH/KR/EN.

Kept:
- JP supported=false for masterdata / assetLists.
- JP masterdata / assetLists direct request fallback behavior.
- WebUI local card-thumbnail hit while preserving online 303 fallback.
- WebUI card multilingual search.
- ZH-CHT CSV translation/glossary sync.
- JP local bundled asset path lookup.

Do not stack this on v12/v13. Apply to your current source as a cumulative patch
and rebuild libew.so/APK.
