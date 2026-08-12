/**
 * Abstract base types for installing and migrating a vendor-neutral XML schema definition into a
 * live database.
 *
 * <p>{@link com.stano.schema.installer.SchemaInstaller} parses a schema (or executes raw SQL),
 * generates dialect-specific DDL, and runs it against a JDBC connection or {@link
 * javax.sql.DataSource}, delegating the actual SQL and migration script execution to a concrete
 * subclass. Concrete implementations backed by Flyway and Liquibase live in the {@code
 * schema-installer-flyway} and {@code schema-installer-liquibase} modules, respectively. Where a
 * schema definition comes from, and how installation state is tracked, is described by types in the
 * {@link com.stano.schema.installer.schemacontext} sub-package.
 */
package com.stano.schema.installer;
