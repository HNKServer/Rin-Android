# v16 JP /api/start WebUI-fallback fix

Base:
- v15 rollback-to-v11b.

This is a real bug-fix candidate, not another rollback.

New change:
- Only in router.rs/webui_fallback:
  /api/start and /api/start/assetHash are treated as game startup APIs even if
  this particular POST lacks the aoharu-asset-version header.

Why:
- The current modular router puts start::routes inside the WebUI fallback scope.
- webui_fallback used to classify any request without aoharu-asset-version as a
  browser request, except /api/webui.
- For /api/start, that means webui::main() returns a 302 redirect to /.
- log20 shows the JP failure happens immediately after:
    Request: POST /api/start
    Curl error 65: necessary data rewind wasn't possible
- libcurl error 65 is consistent with a POST body hitting an unexpected redirect.

What is deliberately NOT changed:
- start.rs is untouched.
- asset hash logic is untouched.
- masterdata/assetLists behavior from v11b/v15 is untouched.
- WebUI local card-thumbnail fallback is untouched.
- WebUI card multilingual search is untouched.
- ZH-CHT translation/glossary sync is untouched.
- JP local static asset lookup is untouched.
- This does not classify all /api/* as game traffic; only the two startup APIs.

Expected effect:
- JP startup image behavior should stay as in v15/v11b.
- WebUI image/search/multilingual changes should stay as in v15/v11b.
- /api/start should no longer be redirected to WebUI when the startup POST lacks
  aoharu-asset-version.
