# v18 JP login/GREE fallback fix

Base:
- v17b.

Why:
- log22 proves /api/start now enters the real handler:
    Request: POST /api/start
    Handle: POST /api/start
    Curl error 65
- Therefore the remaining failure is not WebUI fallback.
- In start(), the first fragile operation is global::get_login().
- For a rebuilt coexist AndroidEw package, the data directory/gree.db may be empty
  even though the official release APK's data directory already had valid GREE
  cert/uuid rows.
- The old code could panic or abort the request when GREE lookup failed:
    assert!(rv != String::new())
    DATABASE.lock_and_select(...).unwrap()

Changes:
1. database/gree.rs:
   Missing cert row now returns String::new() instead of unwrap panic.
2. router/global.rs:
   get_login keeps original a6573cbe UUID path and GREE path, but if both fail
   it falls back to a stable local account token rather than aborting /api/start.

Not changed:
- /api/start response shape.
- asset hash logic.
- masterdata/assetLists logic.
- WebUI image fallback.
- WebUI multilingual search.
- ZH-CHT translations.
- JP static asset lookup.

Expected effect:
- /api/start should return a normal encrypted response instead of dropping the
  connection if the rebuilt package lacks the official APK's GREE database rows.
