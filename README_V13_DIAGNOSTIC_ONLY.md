# v13 diagnostic-only patch

Base: v11b.

This patch is behavior-neutral: it does not change routing, masterdata, asset lists, asset hash, WebUI, or CDN behavior.

It only logs request/response diagnostics for:
- /api/start
- /api/start/assetHash
- /api/masterdata/supported
- /api/assetLists/supported

For each of these it logs status, Location header, request aoharu-asset-version presence, content type, request content length, and response content length.

Use this to prove whether /api/start is being redirected/fallbacked or whether it enters the normal handler and still returns a bad response.
