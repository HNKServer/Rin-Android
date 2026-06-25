# v17 JP /api/start default-service fallback fix

Base:
- v16 start-fallback fix.

Why v16 was not enough:
- v16 fixed the modular webui_fallback middleware path.
- log21 still shows the error immediately after POST /api/start.
- The source also has a second legacy/default entrypoint:
  App::default_service(... router::request)
- router::request has its own no-aoharu-asset-version => WebUI fallback.
  If /api/start falls through to default_service, v16 cannot help.

New changes:
1. Keep v16's middleware exception.
2. Add the same startup exception to router::request's no-header fallback.
3. Add explicit fallback matches:
   - /api/start => start::start(...)
   - /api/start/assetHash => start::asset_hash(...)
4. Make those two start handlers public and add Android logcat handler markers:
   - Handle: POST /api/start
   - Handle: POST /api/start/assetHash

Not changed:
- start.rs response body logic is unchanged.
- asset hash logic is unchanged.
- masterdata/assetLists logic is unchanged.
- WebUI thumbnail/image fallback is unchanged.
- WebUI multilingual card search is unchanged.
- ZH-CHT CSV translation/glossary sync is unchanged.
- JP static asset lookup is unchanged.

Expected log if fixed path is used:
- After Request: POST /api/start, you should see:
  I/ew: Handle: POST /api/start
