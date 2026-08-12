package com.stano.schema.genmigration.impl.common;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;

/**
 * Bundle of options used to configure a {@link MigrationGenerator}: the changeset to generate SQL
 * for, the destination writer, the target database dialect, the statement separator, and an
 * optional {@link Schema} for dialect implementations that need additional context.
 */
public class MigrationGeneratorOptions {
  private final ChangeSet changeSet;
  private final PrintWriter writer;
  private final DatabaseType databaseType;
  private final String statementSeparator;
  private final Schema schema;

  /**
   * Creates options with an explicit statement separator and no associated {@link Schema}.
   *
   * @param changeSet the set of schema changes to generate SQL for
   * @param writer the destination for the generated SQL statements
   * @param databaseType the target database dialect
   * @param statementSeparator the separator appended after each generated SQL statement
   */
  public MigrationGeneratorOptions(
      ChangeSet changeSet,
      PrintWriter writer,
      DatabaseType databaseType,
      String statementSeparator) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = statementSeparator;
    this.schema = null;
  }

  /**
   * Creates options with no associated {@link Schema}, defaulting the statement separator to {@link
   * DatabaseType#getStatementSeparator()}.
   *
   * @param changeSet the set of schema changes to generate SQL for
   * @param writer the destination for the generated SQL statements
   * @param databaseType the target database dialect
   */
  public MigrationGeneratorOptions(
      ChangeSet changeSet, PrintWriter writer, DatabaseType databaseType) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = databaseType.getStatementSeparator();
    this.schema = null;
  }

  /**
   * Creates options with an associated {@link Schema}, defaulting the statement separator to {@link
   * DatabaseType#getStatementSeparator()}.
   *
   * @param changeSet the set of schema changes to generate SQL for
   * @param writer the destination for the generated SQL statements
   * @param databaseType the target database dialect
   * @param schema the schema associated with these options
   */
  public MigrationGeneratorOptions(
      ChangeSet changeSet, PrintWriter writer, DatabaseType databaseType, Schema schema) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = databaseType.getStatementSeparator();
    this.schema = schema;
  }

  /**
   * Creates options with both an explicit statement separator and an associated {@link Schema}.
   *
   * @param changeSet the set of schema changes to generate SQL for
   * @param writer the destination for the generated SQL statements
   * @param databaseType the target database dialect
   * @param statementSeparator the separator appended after each generated SQL statement
   * @param schema the schema associated with these options
   */
  public MigrationGeneratorOptions(
      ChangeSet changeSet,
      PrintWriter writer,
      DatabaseType databaseType,
      String statementSeparator,
      Schema schema) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = statementSeparator;
    this.schema = schema;
  }

  /**
   * Returns the set of schema changes to generate SQL for.
   *
   * @return the changeset
   */
  public ChangeSet getChangeSet() {
    return changeSet;
  }

  /**
   * Returns the destination for the generated SQL statements.
   *
   * @return the writer
   */
  public PrintWriter getWriter() {
    return writer;
  }

  /**
   * Returns the target database dialect.
   *
   * @return the database type
   */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }

  /**
   * Returns the separator appended after each generated SQL statement.
   *
   * @return the statement separator
   */
  public String getStatementSeparator() {
    return statementSeparator;
  }

  /**
   * Returns the schema associated with these options, or {@code null} if none was supplied.
   *
   * @return the associated schema, or {@code null}
   */
  public Schema getSchema() {
    return schema;
  }
}
