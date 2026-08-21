package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;

/**
 * Bundles the configuration settings that control how a {@link SQLGenerator} produces SQL DDL for a
 * schema: the schema and destination writer, the target {@link DatabaseType}, the {@link
 * ForeignKeyMode} and {@link BooleanMode} to use, which parts of the schema to output ({@link
 * OutputMode}), the statement separator to write after each generated statement, the target
 * PostgreSQL major version, whether to emit the PostgreSQL {@code create extension} block, and
 * which role that block's permission check should use. Instances are immutable once constructed.
 */
public class SQLGeneratorOptions {

  private final Schema schema;
  private final PrintWriter sqlWriter;
  private final DatabaseType databaseType;
  private final ForeignKeyMode foreignKeyMode;
  private final BooleanMode booleanMode;
  private final OutputMode outputMode;
  private final String statementSeparator;
  private final int targetPostgresVersion;
  private final boolean emitPostgresExtensions;
  private final String extensionCheckUser;

  /**
   * Creates a new options instance using the database type's default statement separator and the
   * default target PostgreSQL version ({@code 0}).
   *
   * @param schema the schema to generate SQL from
   * @param sqlWriter the writer to which the generated SQL is written
   * @param databaseType the target database dialect
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   */
  public SQLGeneratorOptions(
      Schema schema,
      PrintWriter sqlWriter,
      DatabaseType databaseType,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode) {

    this(
        schema,
        sqlWriter,
        databaseType,
        foreignKeyMode,
        booleanMode,
        outputMode,
        databaseType.getStatementSeparator());
  }

  /**
   * Creates a new options instance using the default target PostgreSQL version ({@code 0}).
   *
   * @param schema the schema to generate SQL from
   * @param sqlWriter the writer to which the generated SQL is written
   * @param databaseType the target database dialect
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   * @param statementSeparator the separator to write after each generated SQL statement
   */
  public SQLGeneratorOptions(
      Schema schema,
      PrintWriter sqlWriter,
      DatabaseType databaseType,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode,
      String statementSeparator) {

    this(
        schema,
        sqlWriter,
        databaseType,
        foreignKeyMode,
        booleanMode,
        outputMode,
        statementSeparator,
        0);
  }

  /**
   * Creates a new options instance, emitting the PostgreSQL {@code create extension} block by
   * default.
   *
   * @param schema the schema to generate SQL from
   * @param sqlWriter the writer to which the generated SQL is written
   * @param databaseType the target database dialect
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   * @param statementSeparator the separator to write after each generated SQL statement
   * @param targetPostgresVersion the target PostgreSQL major version (e.g. 17, 18), or {@code 0} to
   *     use the default
   */
  public SQLGeneratorOptions(
      Schema schema,
      PrintWriter sqlWriter,
      DatabaseType databaseType,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode,
      String statementSeparator,
      int targetPostgresVersion) {

    this(
        schema,
        sqlWriter,
        databaseType,
        foreignKeyMode,
        booleanMode,
        outputMode,
        statementSeparator,
        targetPostgresVersion,
        true);
  }

  /**
   * Creates a new options instance, checking {@code CURRENT_USER}'s privilege in the create
   * extension block by default.
   *
   * @param schema the schema to generate SQL from
   * @param sqlWriter the writer to which the generated SQL is written
   * @param databaseType the target database dialect
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   * @param statementSeparator the separator to write after each generated SQL statement
   * @param targetPostgresVersion the target PostgreSQL major version (e.g. 17, 18), or {@code 0} to
   *     use the default
   * @param emitPostgresExtensions whether to emit the PostgreSQL {@code create extension} block
   *     (citext, btree_gist); ignored for non-PostgreSQL dialects
   */
  public SQLGeneratorOptions(
      Schema schema,
      PrintWriter sqlWriter,
      DatabaseType databaseType,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode,
      String statementSeparator,
      int targetPostgresVersion,
      boolean emitPostgresExtensions) {

    this(
        schema,
        sqlWriter,
        databaseType,
        foreignKeyMode,
        booleanMode,
        outputMode,
        statementSeparator,
        targetPostgresVersion,
        emitPostgresExtensions,
        null);
  }

  /**
   * Creates a new options instance with all settings specified explicitly.
   *
   * @param schema the schema to generate SQL from
   * @param sqlWriter the writer to which the generated SQL is written
   * @param databaseType the target database dialect
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   * @param statementSeparator the separator to write after each generated SQL statement
   * @param targetPostgresVersion the target PostgreSQL major version (e.g. 17, 18), or {@code 0} to
   *     use the default
   * @param emitPostgresExtensions whether to emit the PostgreSQL {@code create extension} block
   *     (citext, btree_gist); ignored for non-PostgreSQL dialects
   * @param extensionCheckUser the Postgres role whose superuser privilege is checked in the create
   *     extension block, or {@code null} to check {@code CURRENT_USER}
   */
  public SQLGeneratorOptions(
      Schema schema,
      PrintWriter sqlWriter,
      DatabaseType databaseType,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode,
      String statementSeparator,
      int targetPostgresVersion,
      boolean emitPostgresExtensions,
      String extensionCheckUser) {

    this.schema = schema;
    this.sqlWriter = sqlWriter;
    this.databaseType = databaseType;
    this.foreignKeyMode = foreignKeyMode;
    this.booleanMode = booleanMode;
    this.outputMode = outputMode;
    this.statementSeparator = statementSeparator;
    this.targetPostgresVersion = targetPostgresVersion;
    this.emitPostgresExtensions = emitPostgresExtensions;
    this.extensionCheckUser = extensionCheckUser;
  }

  /**
   * Returns the schema to generate SQL from.
   *
   * @return the schema
   */
  public Schema getSchema() {

    return schema;
  }

  /**
   * Returns the writer to which the generated SQL is written.
   *
   * @return the SQL writer
   */
  public PrintWriter getSqlWriter() {

    return sqlWriter;
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
   * Returns how foreign keys should be expressed in the generated SQL. If the configured mode is
   * {@link ForeignKeyMode#TRIGGERS} but the target {@link #getDatabaseType() database type} does
   * not support triggers, {@link ForeignKeyMode#RELATIONS} is returned instead.
   *
   * @return the effective foreign key mode
   */
  public ForeignKeyMode getForeignKeyMode() {

    if (foreignKeyMode == ForeignKeyMode.TRIGGERS && !databaseType.isSupportsTriggers()) {
      return ForeignKeyMode.RELATIONS;
    }

    return foreignKeyMode;
  }

  /**
   * Returns how boolean columns should be expressed in the generated SQL.
   *
   * @return the boolean mode
   */
  public BooleanMode getBooleanMode() {

    return booleanMode;
  }

  /**
   * Returns which parts of the schema should be output.
   *
   * @return the output mode
   */
  public OutputMode getOutputMode() {

    return outputMode;
  }

  /**
   * Returns the separator written after each generated SQL statement.
   *
   * @return the statement separator
   */
  public String getStatementSeparator() {

    return statementSeparator;
  }

  /**
   * Returns the target PostgreSQL major version.
   *
   * @return the target PostgreSQL major version (e.g. 17, 18), or {@code 0} if none was specified
   */
  public int getTargetPostgresVersion() {

    return targetPostgresVersion;
  }

  /**
   * Returns whether the PostgreSQL {@code create extension} block (citext, btree_gist) should be
   * emitted. Ignored for non-PostgreSQL dialects.
   *
   * @return {@code true} if the create extension block should be emitted
   */
  public boolean isEmitPostgresExtensions() {

    return emitPostgresExtensions;
  }

  /**
   * Returns the Postgres role whose superuser privilege is checked in the create extension block.
   *
   * @return the role name to check, or {@code null} to check {@code CURRENT_USER}
   */
  public String getExtensionCheckUser() {

    return extensionCheckUser;
  }
}
