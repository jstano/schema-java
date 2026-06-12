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
import javax.sql.DataSource;

public abstract class SchemaInstaller {
  protected static DatabaseType detectDatabaseType(Connection connection) {
    try {
      String url = connection.getMetaData().getURL().toLowerCase();
      if (url.startsWith("jdbc:postgresql")) return DatabaseType.POSTGRES;
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

  public void installSchema(DataSource dataSource, SchemaContext schemaContext) {
    try (Connection connection = dataSource.getConnection()) {
      installSchema(connection, schemaContext);
    } catch (SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

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

  public void migrateSchema(DataSource dataSource, SchemaContext schemaContext) {
    try (Connection connection = dataSource.getConnection()) {
      migrateSchema(connection, schemaContext);
    } catch (SQLException x) {
      throw new SchemaMigrationException(x);
    }
  }

  public void migrateSchema(Connection connection, SchemaContext schemaContext) {
    String migrationScriptLocator = schemaContext.getMigrationScriptLocator(connection);

    if (migrationScriptLocator == null) {
      return;
    }

    DatabaseType databaseType = detectDatabaseType(connection);
    executeMigrationScripts(connection, databaseType, migrationScriptLocator);
  }

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

  protected void executeMigrationScripts(
      Connection connection, DatabaseType databaseType, String locator) {}

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
