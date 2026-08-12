/**
 * Generates dialect-specific migration SQL (CREATE/ALTER/DROP statements) from a {@code
 * com.stano.schema.diff.ChangeSet}.
 *
 * <p>{@link com.stano.schema.genmigration.GenMigration} is the module's entry point: it accepts a
 * changeset directly, or derives one from two {@code com.stano.schema.model.Schema} instances, and
 * delegates to a dialect-specific {@code MigrationGenerator} (see {@code impl.common} and its
 * per-database sub-packages) to write the resulting SQL statements.
 */
package com.stano.schema.genmigration;
