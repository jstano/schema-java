package com.stano.schema.installer.flyway;

import com.stano.schema.installer.SchemaInstaller;
import com.stano.schema.installer.schemacontext.SchemaContext;
import com.stano.schema.model.DatabaseType;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;

/**
 * {@link SchemaInstaller} implementation that generates and executes SQL, and runs migration
 * scripts, using <a href="https://flywaydb.org/">Flyway</a>.
 *
 * <p>SQL files are executed via a {@link FlywayMigrationExecutor}, which supports PostgreSQL, SQL
 * Server, and H2 (per the {@link com.stano.schema.model.DatabaseType} values detected by {@code
 * SchemaInstaller}). When no explicit migration script locator is supplied by the {@code
 * SchemaContext}, migration scripts are looked up on the classpath under {@code db/migration}.
 */
public class FlywaySchemaInstaller extends SchemaInstaller {
  private FlywayMigrationExecutor flywayMigrationExecutor = new FlywayMigrationExecutor();

  /**
   * Replaces the {@link FlywayMigrationExecutor} used to execute SQL files and migration scripts.
   *
   * @param flywayMigrationExecutor the executor to use
   */
  public void setFlywayMigrationExecutor(FlywayMigrationExecutor flywayMigrationExecutor) {
    this.flywayMigrationExecutor = flywayMigrationExecutor;
  }

  @Override
  protected String getDefaultMigrationScriptLocator() {
    return "db/migration";
  }

  @Override
  protected void executeSqlFile(
      Connection connection, DatabaseType databaseType, SchemaContext schemaContext, File sqlFile)
      throws IOException {
    flywayMigrationExecutor.executeSqlFile(databaseType, sqlFile, connection);
  }

  @Override
  protected void executePostCreateScript(Connection connection, String postCreateResourceName) {
    DatabaseType databaseType = detectDatabaseType(connection);
    flywayMigrationExecutor.executeClasspathSqlLocation(
        databaseType, postCreateResourceName, connection);
  }

  @Override
  protected void executeMigrationScripts(
      Connection connection, DatabaseType databaseType, String locator) {
    flywayMigrationExecutor.executeMigrationScripts(databaseType, locator, connection);
  }

  @Override
  protected List<String> findPendingMigrations(
      Connection connection, DatabaseType databaseType, String locator) {
    return flywayMigrationExecutor.getPendingMigrations(databaseType, locator, connection);
  }
}
