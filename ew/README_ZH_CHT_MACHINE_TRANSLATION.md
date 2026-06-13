# ZH-CHT Machine-Translated Masterdata Build

This build adds bundled Traditional Chinese (`ZH-CHT`) masterdata generated from the English CSV set.

Scope:
- Generated `src/router/databases/csv-zh-cht/*.csv` from `csv-en/*.csv`.
- Translated known UI/game text fields such as names, titles, messages, summaries, details, caution text, mission text, and tutorial text.
- Preserved placeholders (`{0}`), HTML tags (`<br>`), asset names, file names, URLs, IDs, cue names, and numeric fields.
- Updated ew's bundled masterdata loader so `ZH-CHT` can use the bundled `csv-zh-cht` folder directly; external masterdata remains optional override.
- Synced the patched ew source into AndroidEw.

Important limitation:
This is an offline batch machine-translation pass, not a manually proofread official localization. Song titles, asset identifiers, markup-heavy text, and many proper nouns are intentionally preserved or only partially translated to avoid breaking client parsing.

Stats:
- CSV files generated: 180
- Candidate text cells processed: 52079
- Cells changed by translation rules: 35450
