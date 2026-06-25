# v8b WebUI multilingual card search only

This is a cumulative patch based on v7:

- Keeps v7 JP/GL asset hash fix.
- Keeps v7 Traditional Chinese translation/glossary sync CSV data.
- Adds WebUI card database multilingual search.
- Does NOT change WebUI card thumbnail serving logic. The original local image lookup plus 303 fallback to sif2-api.ethanthesleepy.one is preserved.

Search fix details:

1. URL-decodes the `query=` parameter, so Chinese/Japanese search terms no longer remain percent-encoded.
2. Builds a card search index across JP / EN / KR / ZH-CHT card and character tables.
3. Allows searching by card name, character name, rarity, attribute, id, masterCharacterId, and illustId across languages.

Card thumbnails should be handled by the separate no-code thumbnail downloader/resource pack, not by changing webui.rs fallback behavior.
