package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for dialect-specific SQL DDL generators.
 *
 * <p>Each supported database dialect provides a concrete subclass in its own sub-package:
 * PostgreSQL in {@code com.stano.schema.gensql.impl.postgresql}, SQL Server in {@code
 * com.stano.schema.gensql.impl.sqlserver}, and H2 in {@code com.stano.schema.gensql.impl.h2}.
 * Subclasses are obtained via {@link SQLGeneratorFactory} rather than constructed directly.
 *
 * <p>{@link #generate()} drives the overall generation process: it validates the schema, then
 * delegates to the abstract {@code output*} methods (implemented by subclasses) to emit the various
 * kinds of DDL, in an order and selection controlled by the configured {@link OutputMode} and
 * {@link ForeignKeyMode}.
 */
public abstract class SQLGenerator {
  private static final Logger logger = LoggerFactory.getLogger(SQLGenerator.class);

  private final SQLGeneratorOptions sqlGeneratorOptions;

  protected final Schema schema;
  protected final PrintWriter sqlWriter;
  protected final String statementSeparator;
  protected final DatabaseType databaseType;

  /**
   * Creates a new generator, extracting the commonly used fields ({@link #schema}, {@link
   * #sqlWriter}, {@link #statementSeparator}, {@link #databaseType}) from the supplied options.
   *
   * @param sqlGeneratorOptions the options controlling how SQL is generated
   */
  protected SQLGenerator(SQLGeneratorOptions sqlGeneratorOptions) {
    this.sqlGeneratorOptions = sqlGeneratorOptions;
    this.schema = sqlGeneratorOptions.getSchema();
    this.sqlWriter = sqlGeneratorOptions.getSqlWriter();
    this.statementSeparator = sqlGeneratorOptions.getStatementSeparator();
    this.databaseType = sqlGeneratorOptions.getDatabaseType();
  }

  /**
   * Returns the options that this generator was created with.
   *
   * @return the {@link SQLGeneratorOptions} passed to the constructor
   */
  public SQLGeneratorOptions getSqlGeneratorOptions() {
    return sqlGeneratorOptions;
  }

  /**
   * Validates the schema and, if valid, generates the SQL DDL by writing it to the configured
   * writer. If the schema fails validation, the validation errors are logged and nothing is
   * written.
   */
  public void generate() {
    if (logger.isDebugEnabled()) {
      logger.debug(
          String.format("Generating SQL for '%s'", schema.getSchemaURL().toExternalForm()));
    }

    if (schemaIsValid()) {
      outputSQL();
    }
  }

  private boolean schemaIsValid() {
    List<String> errors = schema.validate();

    if (!errors.isEmpty()) {
      if (logger.isErrorEnabled()) {
        for (String error : errors) {
          logger.error(error);
        }
      }

      return false;
    }

    return true;
  }

  private void outputSQL() {
    String currentLineSeparator = System.setProperty("line.separator", "\n");

    try {
      try {
        outputHeader();

        if (sqlGeneratorOptions.getOutputMode() == OutputMode.INDEXES_ONLY) {
          outputIndexes();
        } else if (sqlGeneratorOptions.getOutputMode() == OutputMode.TRIGGERS_ONLY) {
          outputTriggers();
        } else {
          outputOtherSqlTop();
          outputTables();

          if (sqlGeneratorOptions.getForeignKeyMode() == ForeignKeyMode.RELATIONS) {
            outputRelations();
          }

          outputTriggers();
          outputFunctions();
          outputViews();
          outputProcedures();
          outputOtherSqlBottom();
        }
      } finally {
        sqlWriter.close();
      }
    } finally {
      System.setProperty("line.separator", currentLineSeparator);
    }
  }

  /**
   * Outputs a header at the top of the generated SQL. The default implementation does nothing;
   * subclasses may override it to emit dialect-specific preamble content.
   */
  protected void outputHeader() {}

  /** Outputs the DDL statements that create the tables and their columns. */
  protected abstract void outputTables();

  /**
   * Outputs the DDL statements that establish foreign key relationships between tables. Only
   * invoked when the configured {@link ForeignKeyMode} is {@link ForeignKeyMode#RELATIONS}.
   */
  protected abstract void outputRelations();

  /** Outputs the DDL statements that create indexes. */
  protected abstract void outputIndexes();

  /**
   * Outputs the DDL statements that create triggers, including any triggers used to enforce foreign
   * keys when the configured {@link ForeignKeyMode} is {@link ForeignKeyMode#TRIGGERS}.
   */
  protected abstract void outputTriggers();

  /** Outputs the DDL statements that create functions. */
  protected abstract void outputFunctions();

  /** Outputs the DDL statements that create views. */
  protected abstract void outputViews();

  /** Outputs the DDL statements that create stored procedures. */
  protected abstract void outputProcedures();

  /** Outputs any other, dialect-specific SQL that should appear before the table DDL. */
  protected abstract void outputOtherSqlTop();

  /** Outputs any other, dialect-specific SQL that should appear after all other generated DDL. */
  protected abstract void outputOtherSqlBottom();
}
