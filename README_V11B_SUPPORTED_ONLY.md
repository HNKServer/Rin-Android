# v11b supported-only JP-safe cumulative patch

Use this if you never applied v9.

Base:
- v10 cumulative patch.

Changes from v10:
- /api/masterdata/supported defaults to unsupported for JP/unknown requests.
- /api/assetLists/supported defaults to unsupported for JP/unknown requests.
- Support is advertised only when the request clearly identifies a non-JP / GL-family client or language.

Intentionally not included:
- No router.rs overwrite.
- No /api/start header-bypass.
- No change to the original WebUI fallback split.

Kept from v10:
- JP/GL assetHash fix.
- JP masterdata / assetLists empty object fallback when directly requested.
- ZH-CHT translation/glossary sync.
- WebUI multilingual card search.
- WebUI local thumbnail lookup with original 303 fallback kept.
- JP local bundled asset alias lookup.
