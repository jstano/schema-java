/**
 * JDBC-based migration helper utilities intended to be called directly from hand-written Flyway or
 * Liquibase Java migration classes.
 *
 * <p>This package provides small, focused helpers for two kinds of tasks that arise when writing
 * imperative database migrations:
 *
 * <ul>
 *   <li><b>Existence checks</b> — determining whether a table, column, index, constraint, or other
 *       database object already exists before conditionally creating or dropping it (e.g. {@link
 *       com.stano.schema.migrations.TableExistsMigration}, {@link
 *       com.stano.schema.migrations.ColumnExistsMigration}, {@link
 *       com.stano.schema.migrations.IndexExistsMigration}, {@link
 *       com.stano.schema.migrations.ConstraintExistsMigration}, {@link
 *       com.stano.schema.migrations.ItemExistsMigration}).
 *   <li><b>Safe DDL operations</b> — dropping indexes, constraints, or triggers only if they exist,
 *       and executing arbitrary SQL (e.g. {@link com.stano.schema.migrations.DropIndexMigration},
 *       {@link com.stano.schema.migrations.DropTableConstraintMigration}, {@link
 *       com.stano.schema.migrations.DropColumnConstraintsMigration}, {@link
 *       com.stano.schema.migrations.DropColumnCheckConstraintMigration}, {@link
 *       com.stano.schema.migrations.DropAllTriggersMigration}, {@link
 *       com.stano.schema.migrations.ExecuteSQLMigration}).
 * </ul>
 *
 * <p>{@link com.stano.schema.migrations.MigrationServices} is the recommended entry point: it is a
 * facade that wraps each of the individual helper classes behind simple methods taking a JDBC
 * {@link java.sql.Connection}. Most of the checks and DDL operations offered by {@code
 * MigrationServices} are backed by SQL Server-specific system catalogs ({@code dbo.sysobjects},
 * {@code dbo.sysindexes}) and the {@code sp_helpconstraint} stored procedure and are therefore only
 * usable against SQL Server; the table and column existence checks use standard JDBC {@link
 * java.sql.DatabaseMetaData} and are portable across database vendors.
 *
 * <p>Individual {@link com.stano.schema.migrations.StatementAction} implementations can also be
 * used directly against an existing {@link java.sql.Statement} when finer control is needed, and
 * checked {@link java.sql.SQLException}s encountered anywhere in this package are wrapped and
 * rethrown as the unchecked {@link com.stano.schema.migrations.MigrationException}.
 */
package com.stano.schema.migrations;
