# v15 rollback-to-v11b cumulative patch

Purpose:
- Stop using v14/v14b.
- Explicitly undo the two v14/v14b changes that regressed behavior:
  - ew/src/router.rs route reshuffle
  - ew/src/router/start.rs legacy-JP asset hash override

Base:
- v11b supported-only cumulative patch.

Kept from v11b:
- JP masterdata/assetLists supported=false.
- JP masterdata/assetLists direct request fallback behavior.
- JP/GL assetHash guard from the multilingual line.
- WebUI local card-thumbnail hit fix while preserving original online 303 fallback.
- WebUI card multilingual search.
- ZH-CHT CSV translation/glossary sync.
- JP local bundled asset path lookup.

Not a new JP communication-error fix:
- This is a rollback/stabilization patch.  It should restore the startup image fix
  and remove the v14/v14b regression before further diagnosis.
