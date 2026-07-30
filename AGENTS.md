# Agent Instructions for java-schema

## Project Overview

**java-schema** is a vendor-neutral relational database schema library. It provides:
- XML-based schema definition format (database-agnostic)
- SQL DDL generation for PostgreSQL, MySQL, SQL Server, and H2
- Live database schema installation via Flyway or Liquibase
- Reverse-engineering of existing databases into XML schemas
- ER diagram generation (Mermaid and PlantUML formats)
- JDBC migration utilities (existence checks, safe table drops, etc.)

**Group ID:** `com.stano` | **Version:** `0.40.0-SNAPSHOT` | **Language:** Java 21

---

## Git Rules

**AI will not automatically run `git add`, `git commit`, or `git push`.** Instead:
- AI can analyze changes and prompt you to commit with suggested messages
- You invoke git operations explicitly (e.g., `! git commit ...` or via manual terminal commands)
- This keeps you in control while allowing AI to help facilitate the workflow

---

## Build & Test

### Quick Commands

```bash
./gradlew build              # compile + test + jacoco coverage (full build)
./gradlew test               # run all tests only
./gradlew :schema-<module>:test  # run tests for a specific module
./gradlew check              # tests + static analysis
```

### Build System
- **Tool:** Gradle 8.14.2 with Kotlin DSL
- **Java Version:** 21 (source and target compatibility)
- **Compiler Flags:** `-parameters` enabled, `-Xlint:unchecked`, `-Xlint:deprecation` (via convention plugin)

---

## Module Structure

All modules are under the `com.stano.schema` package. **Important:** The last three modules are BOMs (platform POMs) and do not contain compiled Java code.

| Module | Purpose |
|---|---|
| `schema-model` | Core domain model: `Schema`, `Table`, `Column`, `Constraint`, `Index`, `View`, `Function`, `Procedure`, `Trigger`, `InitialData`, etc. Minimal dependencies. |
| `schema-parser` | Parses XML schema definition files into the object model. |
| `schema-sql-generator` | Generates SQL DDL statements for multiple database dialects. Per-dialect implementations in `impl/postgresql/`, `impl/mysql/`, `impl/sqlserver/`, `impl/h2/`. |
| `schema-installer` | Abstract base classes and interfaces for schema installation: `SchemaInstaller`, `SchemaContext`, exception types. |
| `schema-installer-flyway` | Concrete `SchemaInstaller` implementation using Flyway for live database installation. |
| `schema-installer-liquibase` | Concrete `SchemaInstaller` implementation using Liquibase for live database installation. |
| `schema-migrations` | JDBC utility helpers: `MigrationServices`, table existence checks, safe drops, migration tracking. |
| `schema-reverse-engineer` | Reverse-engineers a live database schema into XML using JDBC metadata. |
| `schema-diagram-generator` | Generates ER diagrams in Mermaid and PlantUML formats. Per-format implementations in `impl/`. |
| `schema-bom` | Bill of Materials (BOM) for version-aligned dependency management. |
| `schema-platform-dependencies` | Internal platform BOM defining third-party version constraints. |
| `test-platform-dependencies` | Aggregated test dependencies (JUnit 5, Mockito, Logback, etc.). |

---

## Dependency Management Rules

- **Never declare version numbers in module `build.gradle.kts` files.** All versions are pinned in `schema-platform-dependencies/build.gradle.kts`.
- When adding a new third-party library, add the version constraint to `schema-platform-dependencies/build.gradle.kts` first, then reference it by name only in the consuming module.
- When adding a new `schema-*` module, add a constraint for it in `schema-bom/build.gradle.kts`.
- **`commons-logging` is globally excluded** from all configurations — do not add it. Use SLF4J bridges instead.

---

## Code Conventions

### Language
- All implementation is **Java 21**. Do not add Kotlin source files.
- Gradle build scripts use **Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`).

### Package Naming
Use `com.stano.schema.*` with clear domain hierarchy. Per-module packages:
- `com.stano.schema.model` — core model
- `com.stano.schema.parser` — parsing logic
- `com.stano.schema.gensql` — SQL generation (with `impl.postgresql`, `impl.mysql`, etc. sub-packages for dialects)
- `com.stano.schema.installer` — schema installation
- `com.stano.schema.migrations` — migration utilities
- `com.stano.schema.reverseengineer` — reverse-engineering
- `com.stano.schema.gendiagram` — diagram generation

### Code Style

Enforced by `.editorconfig`:
- 2-space indentation (no tabs)
- LF line endings, UTF-8 encoding
- No trailing whitespace, final newline required
- Standard Maven layout: `src/main/java/`, `src/main/resources/`, `src/test/java/`, `src/test/resources/`

**Class design:**
- Plain Java POJOs with mutable state and getters/setters (no Lombok, no records)
- **Build config:** All Java/JaCoCo/publishing config is centralized in `buildSrc/JavaLibraryConventionPlugin`. Module `build.gradle.kts` files are thin—just dependencies and `configurePublishing(...)`.

### Design Patterns in Use
- **Factory Pattern:** `SQLGeneratorFactory` creates per-database `SQLGenerator` implementations; `DiagramGeneratorFactory` for diagram generators.
- **Strategy/Options Object:** `SQLGeneratorOptions` bundles configuration; passed to factories.
- **Template Method:** `SchemaInstaller` abstract base with concrete Flyway/Liquibase subclasses.
- **Interface + Default Implementation:** `SchemaContext` interface with `DefaultSchemaContext` abstract base and `FileSchemaContext` concrete impl.
- **Per-Database Sub-Packages:** Each database dialect has its own sub-package under `impl/` (e.g., `impl.postgresql`, `impl.h2`, `impl.sqlserver`).

---

## Testing Conventions

### Java Unit Tests (`*Test.java` in `src/test/java/`)
- Framework: JUnit 5 (Jupiter) + Mockito
- Annotation: `@DisplayName` on every test class and method (natural language sentences describing intent in camelCase)
- Parameterization: `@ParameterizedTest` with `@CsvSource` for table-driven assertions
- Mocking: `@Mock`, `@ExtendWith(MockitoExtension.class)` for behavior verification
- Assertions: JUnit 5 `Assertions` or Hamcrest matchers

### Test Execution
- **Run under JUnit Platform:** `useJUnitPlatform()` configured in the convention plugin
- **JVM Args for Tests:** `--add-opens java.base/java.lang.reflect` and `java.base/java.lang` (required by Mockito)
- **Code Coverage:** JaCoCo HTML and XML reports generated per module and aggregated at the root (`jacocoRootReport`)

---

## Key File Locations

| File/Directory | Purpose |
|---|---|
| `settings.gradle.kts` | Gradle multi-project declaration |
| `build.gradle.kts` (root) | Root build config |
| `buildSrc/src/main/kotlin/com.stano.schema/JavaLibraryConventionPlugin.kt` | Convention plugin (centralizes all Java/JaCoCo/publishing) |
| `.editorconfig` | Enforces 2-space indent, LF, UTF-8 |
| `.serena/project.yml` | Serena LSP configuration (Java language) |
| `README.md` | User-facing documentation (comprehensive) |
| `docs/plans/` | Architecture decision records and planning docs |
| `publish.sh` | Script to zip per-module staging-deploy directories and upload to Maven Central |

---

## Common Tasks

### Build a single module
```bash
./gradlew :schema-sql-generator:build
./gradlew :schema-parser:test
```

### Run tests with verbose output
```bash
./gradlew test --info
```

### Check test coverage
```bash
./gradlew build && open schema-model/build/reports/jacoco/test/html/index.html
```

### Publish locally (for testing)
```bash
./gradlew publish
```

### Add a new module
1. Create directory: `mkdir schema-new-module`
2. Add `build.gradle.kts` with standard Java library setup
3. Add to `settings.gradle.kts`: `include("schema-new-module")`
4. Follow the convention: thin build config + dependencies, rely on `JavaLibraryConventionPlugin`

---

## Notes for AI Agents

- **Reuse patterns:** When adding SQL generation, extend `SQLGenerator` or use `SQLGeneratorFactory`. For diagrams, use `DiagramGeneratorFactory`.
- **Per-dialect code:** Database-specific logic goes in `impl.<dialect>` sub-packages (e.g., `impl.postgresql`).
- **Test fixtures:** XML schemas are golden fixtures in `src/test/resources/`—update them carefully.
- **Compatibility:** This library supports Java 21+. Do not introduce Java 11-era patterns.
- **No external CLI dependencies:** The library itself is a pure library; CLI tools are separate (if any).
- **Platform BOM pattern:** Use `schema-platform-dependencies` to pin third-party versions; modules depend on `schema-bom` for transitive alignment.
