# Review of schema-rs -> schema-java sync (commits e24fc7a..3a4725c)

## Context
Six "merged changes from schema-rs" commits ported Groups A-G from `../schema-rs/SYNC_TO_SCHEMA_JAVA.md`. Three read-only reviews compared each port with the Rust fix. Findings below came from code reading (no builds, no live DB), except the two items marked "confirmed", which I checked directly. The plan: fix in priority order, test-first, one checklist step per batch so work can stop at any point.

## Findings and fixes (priority order)

### Tier 1 - wrong output today
- [x] **Parser check leak** (confirmed): `TableColumnContentHandler.startElement("column")` never resets `checkConstraint` (field line 21, set line 84), so one column's `<check>` leaks into later columns. Fixture `schema-parser-test-schema-postgres.sql:85-90` shows it. Fix: reset to null on each column; add a two-column parser test.
- [x] **Postgres varchar drops user checks** (confirmed): `PostgreSQLColumnConstraintGenerator.getCheckConstraintSQL` returns only `length(x) <= n` for VARCHAR, never calling `super`. Fix: remove the branch (varchar(n) already enforces length) and update golden SQL fixtures.
- [ ] (deferred, tracked separately) **M7 aggregation triggers still broken** (sync doc wrongly said "already in Java"): `PostgreSQLDeleteTriggerGenerator.java:182-232`, `SQLServerDeleteTriggerGenerator.java:288-310`; port Rust `7cb1c3d` (`postgres_aggregation_sql.rs`, `sqlserver_aggregation_sql.rs`). Largest item - confirm scope first.
- [x] **Enum drop ordering** (diff engine, also a Rust bug): `SchemaDiffEngine.java:104-117` drops enum types before `modifyColumns`; Postgres uses `DROP TYPE ... CASCADE` (`PostgreSQLMigrationGenerator.java:677`), destroying columns. Fix: drop after modifyColumns, drop `CASCADE`.
- [x] **SQL Server ModifyEnumType before new tables/columns exist** (`SQLServerMigrationGenerator.java:731-740`): only cycle CHECKs for columns present in the old schema.

### Tier 2 - medium
- [ ] M3 tests missing: port the 4 Rust tests (user check + min/max, pre-wrapped check, boolean NATIVE, boolean YES_NO).
- [ ] Invalid schema -> empty file, exit 0 (`SQLGenerator.schemaIsValid/generate`, `GenSQL`): make it throw like the other CLI paths.
- [ ] M11 missing-target check unreachable from XML: `SchemaParser.java:75` `buildReverseRelations()` throws before `validate()`.
- [ ] `Key` 2-arg ctor leaves PRIMARY non-unique, so reverse-engineered PK != parsed PK in `SchemaDiffEngine.keysEqual`: use `type != INDEX`.
- [ ] SQL Server delete trigger uses first PK column rather than `relation.getFromColumnName()` (`SQLServerDeleteTriggerGenerator.java:87-91`).
- [ ] Postgres `ModifyColumn` never cycles CHECK constraints (varchar widening leaves old check).
- [ ] Sequence columns: false M25 halt on Postgres; invalid identity DDL on SQL Server (`PostgreSQL...:123`, `SQLServer...:138,206`).
- [ ] SQL Server FK re-add happens per column (fails when both FK sides change type): defer to after all ModifyColumns (also Rust).
- [ ] Removed last trigger never dropped (`MigrationGenerator regenerateTriggers`); `other_sql` TOP/BOTTOM ignored + blank entries emitted; PG `ADD VALUE` then use in same migration (55P04) - at least warn.
- [ ] H2 `ModifyColumn` drop/re-add: replace with `ALTER COLUMN ... SET DATA TYPE / SET [NOT] NULL / SET DEFAULT`.
- [ ] Reverse engineer: fail fast with "PostgreSQL only" for other DBs (sync doc M35 skip is wrong); `populateConstraints` unscoped by schema and name-matching via `regclass` text; constraints written without `databaseType="postgresql"`; ARRAY element types beyond `_text/_int*`.
- [ ] Reverse-engineer warnings go to slf4j with no binding: write to `System.err` (or add runtime binding).
- [ ] CLI: `new URI("file://" + path)` breaks relative paths/spaces in `GenSQL`/`GenDiagram` - use `Path.toUri()`.
- [ ] Mermaid emits `PK FK` for a column that is both (use PK only, like PlantUML).

### Tier 3 - low / hygiene
Vacuous 2nd assertion in `CardinalityTest.compositeNeedsEveryColumnRequired` (cached `columnMap`); `Cardinality.of` case-sensitive column lookup; `ck_`/`df_` names not byte-limited; `Naming` needs `Locale.ROOT`; `stripCheckWrapper` mismatched parens; `generated` validation rejects names like `generated_total*2`; schema param used as LIKE pattern in `SchemaReader`; unknown CLI flags ignored; diagram identifier sanitizing; dependent views recreated too early; nullable->required has no guard; SQL Server `THROW` under sqlcmd; XML 1.0 vs `escapeXml11`; `PrintWriter.checkError()` unchecked.

### Cross-repo
Logged in schema-rs as `M38` in `BUGS_AND_GAPS_2026-09-08.md` (enum drops run before columns move off the type, and use `cascade`; Java fixed in this repo, Rust not yet). Items marked "also Rust" (enum drop order, FK re-add timing, M7 rework if any, check-wrapper stripping, `generated` prefix test, ck_/df_ byte limits) should be fixed in `../schema-rs` too.

## Verified OK (no action)
M3 core logic, M8 Naming port, M9 view ordering, M2 SQL shapes, L4/L5, DONOTHING FK skipping and ordinals, M24, M26, M29, M32 Cardinality, FK rule mapping, `pg_index` query, CDATA/`escapeAttr` round-trip, CLI exit codes, AddTable initial-data duplication.

## Verification
- `./gradlew spotlessApply && ./gradlew build` after each checklist step.
- Add a failing test before each fix (tests named in findings above).
- Reverse-engineer and migration SQL: run against postgres:17 in docker as was done for the pg_index query; H2 changes can be tested in-process.

## Decision
Tier 1 only, M7 tracked separately. Tier 1 (minus M7) done and `./gradlew build` green; Tiers 2-3 not started.
