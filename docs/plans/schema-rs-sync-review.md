# Review of schema-rs -> schema-java sync (commits e24fc7a..3a4725c)

## Context
Six "merged changes from schema-rs" commits ported Groups A-G from `../schema-rs/SYNC_TO_SCHEMA_JAVA.md`. Three read-only reviews compared each port with the Rust fix. Findings below came from code reading (no builds, no live DB), except the two items marked "confirmed", which I checked directly. The plan: fix in priority order, test-first, one checklist step per batch so work can stop at any point.

## Findings and fixes (priority order)

### Tier 1 - wrong output today
- [x] **Parser check leak** (confirmed): `TableColumnContentHandler.startElement("column")` never resets `checkConstraint` (field line 21, set line 84), so one column's `<check>` leaks into later columns. Fixture `schema-parser-test-schema-postgres.sql:85-90` shows it. Fix: reset to null on each column; add a two-column parser test.
- [x] **Postgres varchar drops user checks** (confirmed): `PostgreSQLColumnConstraintGenerator.getCheckConstraintSQL` returns only `length(x) <= n` for VARCHAR, never calling `super`. Fix: remove the branch (varchar(n) already enforces length) and update golden SQL fixtures.
- [x] **M7 aggregation triggers** (sync doc wrongly said "already in Java"): rewrote Postgres (row-level `insert ... on conflict` / `update ... from`) and SQL Server (inserted/deleted CTEs + `merge`/`update`) aggregation SQL, ported from Rust `7cb1c3d`. New `AggregationSql` (common), `PostgreSQLAggregationSql`, `SQLServerAggregationSql`; old methods removed; 8 unit tests added. Postgres verified on postgres:17 (insert/update/bucket move/criteria flip/delete); SQL Server unit tests only. Not ported (same as Rust): the priming proc was only repointed at the new bucket/destination, and no validation that destination tables/columns exist.
- [x] **Enum drop ordering** (diff engine, also a Rust bug): `SchemaDiffEngine.java:104-117` drops enum types before `modifyColumns`; Postgres uses `DROP TYPE ... CASCADE` (`PostgreSQLMigrationGenerator.java:677`), destroying columns. Fix: drop after modifyColumns, drop `CASCADE`.
- [x] **SQL Server ModifyEnumType before new tables/columns exist** (`SQLServerMigrationGenerator.java:731-740`): only cycle CHECKs for columns present in the old schema.

### Tier 2 - medium
- [x] M3 tests missing: port the 4 Rust tests (user check + min/max, pre-wrapped check, boolean NATIVE, boolean YES_NO).
- [x] Invalid schema -> empty file, exit 0 (`SQLGenerator.schemaIsValid/generate`, `GenSQL`): make it throw like the other CLI paths.
- [x] M11 missing-target check unreachable from XML: `SchemaParser.java:75` `buildReverseRelations()` throws before `validate()`.
- [x] `Key` 2-arg ctor leaves PRIMARY non-unique, so reverse-engineered PK != parsed PK in `SchemaDiffEngine.keysEqual`: use `type != INDEX`.
- [x] SQL Server delete trigger uses first PK column rather than `relation.getFromColumnName()` (`SQLServerDeleteTriggerGenerator.java:87-91`).
- [x] Postgres `ModifyColumn` never cycles CHECK constraints (varchar widening leaves old check).
- [x] Sequence columns: false M25 halt on Postgres; invalid identity DDL on SQL Server (`PostgreSQL...:123`, `SQLServer...:138,206`).
- [x] SQL Server FK re-add happens per column (fails when both FK sides change type): defer to after all ModifyColumns (also Rust).
- [x] Removed last trigger never dropped (`MigrationGenerator regenerateTriggers`): new `outputDropObsoleteTriggersForTable` hook drops the trigger/function.
- [x] `other_sql` TOP/BOTTOM ignored + blank entries emitted by migrations; PG `ADD VALUE` then use in same migration (55P04) - at least warn.
- [x] H2 `ModifyColumn` drop/re-add: replace with `ALTER COLUMN ... SET DATA TYPE / SET [NOT] NULL / SET DEFAULT`.
- [x] Reverse engineer: fail fast with "PostgreSQL only" for other DBs (sync doc M35 skip is wrong); `populateConstraints` unscoped by schema and name-matching via `regclass` text; constraints written without `databaseType="postgresql"`; ARRAY element types beyond `_text/_int*`.
- [x] Reverse-engineer warnings go to slf4j with no binding: write to `System.err` (or add runtime binding).
- [x] CLI: `new URI("file://" + path)` breaks relative paths/spaces in `GenSQL`/`GenDiagram` - use `Path.toUri()`.
- [x] Mermaid emits `PK FK` for a column that is both (use PK only, like PlantUML).

### Tier 3 - low / hygiene
Done: `CardinalityTest` vacuous assertion and case-insensitive `Cardinality` lookup; `ck_`/`df_` names byte-limited (`Naming.hashedColumnName`); `Naming` uses `Locale.ROOT`; `stripCheckWrapper` matches its closing paren; `generated` validation only rejects a real clause (`generated always|by|as`); `SchemaReader` escapes the schema name used as a LIKE pattern; `GenSQL` rejects unknown `--` flags (the other CLIs use commons-cli, which already does); diagram identifiers sanitized; dependent views recreated after the whole run of column changes; `escapeXml10` instead of `escapeXml11`; `GenSQL` checks `PrintWriter.checkError()`.

Not changed, by decision: nullable->required (Postgres and SQL Server already fail the `SET NOT NULL` with a clear error if NULL rows exist) and SQL Server `THROW` under sqlcmd (scripts need `sqlcmd -b` to halt; a generator-side fix would only work in sqlcmd, not SSMS/JDBC).

### Cross-repo
Logged in schema-rs as `M38` in `BUGS_AND_GAPS_2026-09-08.md` (enum drops run before columns move off the type, and use `cascade`; Java fixed in this repo, Rust not yet). Items marked "also Rust" (enum drop order, FK re-add timing, M7 rework if any, check-wrapper stripping, `generated` prefix test, ck_/df_ byte limits) should be fixed in `../schema-rs` too.

## Verified OK (no action)
M3 core logic, M8 Naming port, M9 view ordering, M2 SQL shapes, L4/L5, DONOTHING FK skipping and ordinals, M24, M26, M29, M32 Cardinality, FK rule mapping, `pg_index` query, CDATA/`escapeAttr` round-trip, CLI exit codes, AddTable initial-data duplication.

## Verification
- `./gradlew spotlessApply && ./gradlew build` after each checklist step.
- Add a failing test before each fix (tests named in findings above).
- Reverse-engineer and migration SQL: run against postgres:17 in docker as was done for the pg_index query; H2 changes can be tested in-process.

## Decision
Tier 1 only plus M7. Tier 1 and M7 done and `./gradlew build` green; Tier 2 done. Tier 3 done except the two items noted above.
