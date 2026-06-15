# ZH-CHT Public Wiki Corrected v4

This v4 language pass stops relying on the broken `ZH-Android.7z` extraction path.

Main changes:

- Keep WebUI/masterdata language key as `ZH-CHT`.
- Use public Love Live reference sites as a correction layer for names and terminology.
- For song titles, prefer attested Chinese fandom/wiki titles when high-confidence; otherwise keep the official original title instead of inventing unstable machine translations.
- Correct member names for μ's, Aqours, Nijigasaki, Liella!, and Yohane fantasy names.
- Fix `new_skill.csv`: skill names now follow card titles instead of leaking English names, and all 15 skill-description templates are clean Traditional Chinese.
- Add missing `multi_penalty.csv` to `csv-zh-cht` so the table set is complete.

Important limitation:

Public wikis do not provide a complete, authoritative Traditional Chinese SIF2 card title table. For card titles without a reliable source, v4 avoids over-translation and uses the official original Japanese title rather than the earlier broken mixed machine translation.
