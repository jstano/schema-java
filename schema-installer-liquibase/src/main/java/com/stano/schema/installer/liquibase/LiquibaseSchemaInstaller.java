package com.stano.schema.installer.liquibase;

import com.stano.schema.installer.SchemaInstaller;
import com.stano.schema.installer.schemacontext.SchemaContext;
import com.stano.schema.model.DatabaseType;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.logging.Logger;

public class LiquibaseSchemaInstaller extends SchemaInstaller {
  private static final Logger log = Logger.getLogger(LiquibaseSchemaInstaller.class.getName());

  private LiquibaseChangeLogCreator liquibaseChangeLogCreator = new LiquibaseChangeLogCreator();
  private LiquibaseChangeLogExecutor liquibaseChangeLogExecutor = new LiquibaseChangeLogExecutor();

  @Override
  protected String getDefaultMigrationScriptLocator() {
    return "db/changelog/db.changelog-master.xml";
  }

  @Override
  protected void executeSqlFile(
      Connection connection, DatabaseType databaseType, SchemaContext schemaContext, File sqlFile)
      throws IOException {
    File tempChangeLogFile =
        createTempChangeLogFile(databaseType, sqlFile, databaseType.getStatementSeparator());
    executeTempChangeLog(connection, tempChangeLogFile);
  }

  @Override
  protected void executePostCreateScript(Connection connection, String postCreateResourceName) {
    liquibaseChangeLogExecutor.executeChangeLog(postCreateResourceName, connection);
  }

  @Override
  protected void executeMigrationScripts(
      Connection connection, DatabaseType databaseType, String locator) {
    liquibaseChangeLogExecutor.executeChangeLog(locator, connection);
  }

  private File createTempChangeLogFile(
      DatabaseType databaseType, File tempSqlFile, String endDelimiter) throws IOException {
    return liquibaseChangeLogCreator.createTempChangeLogFile(
        databaseType, tempSqlFile, endDelimiter);
  }

  private void executeTempChangeLog(Connection connection, File tempChangeLogFile) {
    try {
      liquibaseChangeLogExecutor.executeChangeLog(tempChangeLogFile, connection);
    } finally {
      if (!tempChangeLogFile.delete()) {
        log.warning("Failed to delete temp changelog file: " + tempChangeLogFile.getAbsolutePath());
      }
    }
  }
}
