package com.stano.schema.installer;

import com.stano.schema.gensql.GenSQL;
import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.installer.schemacontext.SchemaContext;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

/**
 * Abstract base type used to install and migrate a vendor-neutral XML schema definition into a live
 * database.
 *
 * <p>A {@code SchemaInstaller} parses the schema referenced by a {@link SchemaContext}, generates
 * SQL DDL for the target database (detected from the JDBC connection metadata), and executes it
 * against that database. It also supports running migration scripts and post-create scripts, and
 * reporting which migrations are still pending.
 *
 * <p>This class has no behavior of its own for executing SQL or migration scripts; concrete
 * subclasses supply that behavior. Two implementations are provided: {@code FlywaySchemaInstaller}
 * (module {@code schema-installer-flyway}), which executes SQL and migrations using Flyway, and
 * {@code LiquibaseSchemaInstaller} (module {@code schema-installer-liquibase}), which does so using
 * Liquibase.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * SchemaInstaller installer = new FlywaySchemaInstaller();
 * installer.installSchema(dataSource, new FileSchemaContext(new File("my-schema.xml")));
 * }</pre>
 */
public abstract class SchemaInstaller {
  /**
   * Determines the {@link DatabaseType} of the database on the other end of the given JDBC
   * connection by inspecting its JDBC URL.
   *
   * @param connection the connection whose database type should be detected
   * @return the detected database type
   * @throws IllegalArgumentException if the JDBC URL does not correspond to a supported database
   * @throws SchemaMigrationException if the database metadata cannot be read
   */
  protected static DatabaseType detectDatabaseType(Connection connection) {
    try {
      String url = connection.getMetaData().getURL().toLowerCase();
      if (url.startsWith("jdbc:postgresql")) return DatabaseType.POSTGRESQL;
      if (url.startsWith("jdbc:sqlserver")) return DatabaseType.SQL_SERVER;
      if (url.startsWith("jdbc:h2")) return DatabaseType.H2;
      throw new IllegalArgumentException("Unsupported JDBC URL: " + url);
    } catch (SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  private static final String TEMP_SQL_FILE_PREFIX = "_temp_sql_";
  private static final String TEMP_SQL_FILE_EXTENSION = ".sql";

  private GenSQL genSQL = new GenSQL();
  private SchemaParser schemaParser = new SchemaParser();

  /**
   * Installs the schema described by the given {@link SchemaContext} into the database reachable
   * through the given data source, unless it has already been installed.
   *
   * <p>Obtains a connection from the data source and delegates to {@link #installSchema(Connection,
   * SchemaContext)}, closing the connection afterward.
   *
   * @param dataSource the data source used to obtain a connection to the target database
   * @param schemaContext describes the schema to install and how to install it
   * @throws SchemaMigrationException if a connection cannot be obtained or installation fails
   */
  public void installSchema(DataSource dataSource, SchemaContext schemaContext) {
    try (Connection connection = dataSource.getConnection()) {
      installSchema(connection, schemaContext);
    } catch (SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  /**
   * Installs the schema described by the given {@link SchemaContext} into the database reachable
   * through the given connection, unless it has already been installed.
   *
   * <p>If {@link SchemaContext#schemaIsInstalled(Connection)} reports that the schema is already
   * installed, this method returns without doing anything. Otherwise it detects the database type,
   * parses the schema XML, generates SQL DDL to a temporary file, executes that file against the
   * connection, runs any configured post-create script, and finally marks the schema as installed
   * via {@link SchemaContext#schemaInstalled(Connection)}.
   *
   * @param connection the connection to the target database
   * @param schemaContext describes the schema to install and how to install it
   * @throws SchemaMigrationException if reading, generating, or executing the schema fails
   */
  public void installSchema(Connection connection, SchemaContext schemaContext) {
    try {
      if (schemaContext.schemaIsInstalled(connection)) {
        return;
      }

      DatabaseType databaseType = detectDatabaseType(connection);
      Schema schema = schemaParser.parseSchema(schemaContext.getSchemaUrl());

      File tempSqlFile =
          File.createTempFile(
              TEMP_SQL_FILE_PREFIX, databaseType.name().toLowerCase() + TEMP_SQL_FILE_EXTENSION);
      generateSqlToTempSqlFile(databaseType, schemaContext, schema, tempSqlFile);

      executeSqlFile(connection, databaseType, schemaContext, tempSqlFile);

      runPostCreateScript(schemaContext, connection);

      schemaContext.schemaInstalled(connection);
    } catch (IOException | SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  /**
   * Runs any pending migration scripts against the database reachable through the given data
   * source.
   *
   * <p>Obtains a connection from the data source and delegates to {@link #migrateSchema(Connection,
   * SchemaContext)}, closing the connection afterward.
   *
   * @param dataSource the data source used to obtain a connection to the target database
   * @param schemaContext describes where migration scripts are located
   * @throws SchemaMigrationException if a connection cannot be obtained or migration fails
   */
  public void migrateSchema(DataSource dataSource, SchemaContext schemaContext) {
    try (Connection connection = dataSource.getConnection()) {
      migrateSchema(connection, schemaContext);
    } catch (SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  /**
   * Runs any pending migration scripts against the database reachable through the given connection.
   *
   * <p>The migration script locator is obtained from {@link
   * SchemaContext#getMigrationScriptLocator(Connection)}, falling back to {@link
   * #getDefaultMigrationScriptLocator()} if that returns {@code null}. If no locator is available,
   * this method does nothing. Otherwise the database type is detected and the migration scripts at
   * that locator are executed via {@link #executeMigrationScripts(Connection, DatabaseType,
   * String)}.
   *
   * @param connection the connection to the target database
   * @param schemaContext describes where migration scripts are located
   */
  public void migrateSchema(Connection connection, SchemaContext schemaContext) {
    String migrationScriptLocator = schemaContext.getMigrationScriptLocator(connection);

    if (migrationScriptLocator == null) {
      migrationScriptLocator = getDefaultMigrationScriptLocator();
    }

    if (migrationScriptLocator == null) {
      return;
    }

    DatabaseType databaseType = detectDatabaseType(connection);
    executeMigrationScripts(connection, databaseType, migrationScriptLocator);
  }

  /**
   * Returns the migrations that are pending (not yet applied) for the database reachable through
   * the given data source.
   *
   * <p>Obtains a connection from the data source and delegates to {@link
   * #getPendingMigrations(Connection, SchemaContext)}, closing the connection afterward.
   *
   * @param dataSource the data source used to obtain a connection to the target database
   * @param schemaContext describes where migration scripts are located
   * @return the identifiers of the pending migrations, or an empty list if none are pending or no
   *     migration script locator is available
   * @throws SchemaMigrationException if a connection cannot be obtained
   */
  public List<String> getPendingMigrations(DataSource dataSource, SchemaContext schemaContext) {
    try (Connection connection = dataSource.getConnection()) {
      return getPendingMigrations(connection, schemaContext);
    } catch (SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  /**
   * Returns the migrations that are pending (not yet applied) for the database reachable through
   * the given connection.
   *
   * <p>The migration script locator is obtained from {@link
   * SchemaContext#getMigrationScriptLocator(Connection)}, falling back to {@link
   * #getDefaultMigrationScriptLocator()} if that returns {@code null}. If no locator is available,
   * an empty list is returned. Otherwise the database type is detected and pending migrations are
   * looked up via {@link #findPendingMigrations(Connection, DatabaseType, String)}.
   *
   * @param connection the connection to the target database
   * @param schemaContext describes where migration scripts are located
   * @return the identifiers of the pending migrations, or an empty list if none are pending or no
   *     migration script locator is available
   */
  public List<String> getPendingMigrations(Connection connection, SchemaContext schemaContext) {
    String migrationScriptLocator = schemaContext.getMigrationScriptLocator(connection);

    if (migrationScriptLocator == null) {
      migrationScriptLocator = getDefaultMigrationScriptLocator();
    }

    if (migrationScriptLocator == null) {
      return List.of();
    }

    DatabaseType databaseType = detectDatabaseType(connection);
    return findPendingMigrations(connection, databaseType, migrationScriptLocator);
  }

  /**
   * Installs a raw SQL script (rather than an XML schema definition) into the database reachable
   * through the given data source, unless the schema has already been installed.
   *
   * <p>If {@link SchemaContext#schemaIsInstalled(Connection)} reports that the schema is already
   * installed, this method returns without doing anything. Otherwise it detects the database type,
   * copies the SQL read from {@link SchemaContext#getSchemaUrl()} to a temporary file, executes
   * that file against the connection, and marks the schema as installed via {@link
   * SchemaContext#schemaInstalled(Connection)}.
   *
   * @param dataSource the data source used to obtain a connection to the target database
   * @param schemaContext describes the SQL script to install and how to install it
   * @throws SchemaMigrationException if reading, copying, or executing the SQL script fails
   */
  public void installSql(DataSource dataSource, SchemaContext schemaContext) {
    try {
      try (Connection connection = dataSource.getConnection()) {
        if (schemaContext.schemaIsInstalled(connection)) {
          return;
        }

        DatabaseType databaseType = detectDatabaseType(connection);

        File tempSqlFile =
            File.createTempFile(
                TEMP_SQL_FILE_PREFIX, databaseType.name().toLowerCase() + TEMP_SQL_FILE_EXTENSION);
        generateSqlToTempSqlFile(schemaContext.getSchemaUrl().openStream(), tempSqlFile);

        executeSqlFile(connection, databaseType, schemaContext, tempSqlFile);

        schemaContext.schemaInstalled(connection);
      }
    } catch (IOException | SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  protected String getDefaultMigrationScriptLocator() {
    return null;
  }

  protected void executeMigrationScripts(
      Connection connection, DatabaseType databaseType, String locator) {}

  protected List<String> findPendingMigrations(
      Connection connection, DatabaseType databaseType, String locator) {
    return List.of();
  }

  protected abstract void executeSqlFile(
      Connection connection, DatabaseType databaseType, SchemaContext schemaContext, File sqlFile)
      throws IOException;

  protected abstract void executePostCreateScript(
      Connection connection, String postCreateResourceName);

  private void generateSqlToTempSqlFile(
      DatabaseType databaseType, SchemaContext schemaContext, Schema schema, File tempSqlFile)
      throws IOException {
    ForeignKeyMode foreignKeyMode = schema.getForeignKeyMode();
    BooleanMode booleanMode = schema.getBooleanMode();

    if (foreignKeyMode == null) {
      foreignKeyMode = schemaContext.getForeignKeyMode();
    }

    if (booleanMode == null) {
      booleanMode = schemaContext.getBooleanMode();
    }

    genSQL.generateSQL(
        databaseType,
        schema,
        new PrintWriter(new FileWriter(tempSqlFile)),
        foreignKeyMode,
        booleanMode,
        OutputMode.ALL,
        databaseType.getStatementSeparator());
  }

  private void generateSqlToTempSqlFile(InputStream inputStream, File tempSqlFile)
      throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter writer = new PrintWriter(new FileWriter(tempSqlFile))) {
      String line;

      while ((line = reader.readLine()) != null) {
        writer.println(line);
      }
    }
  }

  private void runPostCreateScript(SchemaContext schemaContext, Connection connection) {
    String postCreateScriptLocator = schemaContext.getPostCreateScriptLocator(connection);

    if (postCreateScriptLocator == null) {
      return;
    }

    executePostCreateScript(connection, postCreateScriptLocator);
  }
}
