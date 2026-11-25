# Changelog

## v1.5.1-resync

### New features
- **SQLite database** — nicks, punishments and audit log persist in `config/stafftools.db` (WAL mode). JSON history auto-migrates on first run.
- **`/stdb` command** — database management:
  - `list [page]` — paginated nick list (clickable, copies on click).
  - `search <text>` / `hardsearch <text>` — substring nick search.
  - `status` — file size, row counts.
  - `reload` — force reload nick cache from DB.
  - `delete <nick>` — remove a nick.
  - `export` — dump full DB to 3 CSV files.
- **Nick autocomplete** — nicks from DB (including offline players) are clickable in chat (opens actions popup) and marked ignored for audit/profiler to avoid spam.
- **Audit DB tab** — filter chip "База данных" shows DB log + stats (file size, counts) in a vertical split.
- **Custom icons** — rail icons generated as PNG textures.
- **Sounds** — distinct UI sounds per action/notification.
- **`<reason>` placeholder** — modal input with accidental-Enter guard, bordered field.
- **Guidebook** — in-game markdown-style help (bottom of rail), now includes `/stdb`.
- **JSON localization** — `assets/stafftools/lang/{ru,en}.json`.

### Improvements
- 12 redesigned themes with distinct surface/border/text tones per accent.
- Hover card polish: gradient header, accent stripe, pulsing status dot.
- Batched audit DB writes (every 5s).
- Nick validation rejects NPC/placeholder names (`#`, `%`, etc.).
- `stdb` output uses theme colors.

### Fixes
- Reason modal text field centered inside its border.
- `/stdb list` shows real DB count.
- Pagination single-line with clickable arrows only.
- `hardsearch` uses reliable `LIKE` contains-match.
- Ignored nicks still open the actions popup on click.

## v1.5.0-resync
- Initial resync baseline.