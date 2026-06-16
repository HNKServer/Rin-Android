{
  "purpose": "Fix ZH asset download 404 after publicwiki v6: restore canonical ZhCht language code to ZH and restore CSV fallback.",
  "root_cause": "v6 changed canonical_lang(Region::ZhCht) from ZH to ZH-CHT. global::get_asset_hash_for_lang only recognizes ZH, so Traditional Chinese clients could be routed to EN asset hash while still requesting /Android/ZH paths, causing missing files/404 against ZH-Android assets.",
  "files": [
    "src/router/master_data.rs",
    "src/router/databases/csv.rs"
  ],
  "changes": {
    "src/router/master_data.rs": "Region::ZhCht => \"ZH\"",
    "src/router/databases/csv.rs": "Region::ZhCht fallback restored to csv-zh-cht, csv-zh, csv-en"
  },
  "usage": {
    "desktop": "copy desktop/* into ew root",
    "android_inner": "copy android_inner/* into AndroidEw inner project root that contains app/ and ew/",
    "android_outer": "copy android_outer/* into outer directory that contains ew-android/"
  }
}