package com.stano.schema.installer.flyway;

import com.stano.schema.model.DatabaseType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

public class FlywayMigrationExecutor {
  private FlywayDatabaseUpgradeLog flywayDatabaseUpgradeLog = new FlywayDatabaseUpgradeLog();

  public void executeSqlFile(DatabaseType databaseType, File sqlFile, Connection connection)
      throws IOException {
    Path tempDir = Files.createTempDirectory("flyway_install_");
    File renamedFile = new File(tempDir.toFile(), "V1__install.sql");

    try {
      Files.copy(sqlFile.toPath(), renamedFile.toPath());
      executeFlyway(
          databaseType,
          "filesystem:" + tempDir.toAbsolutePath(),
          connection,
          sqlFile.getName(),
          "flyway_schema_history_install");
    } finally {
      deleteDirectory(tempDir.toFile());
    }
  }

  public void executeClasspathSqlLocation(
      DatabaseType databaseType, String classpathResource, Connection connection) {
    Path tempDir;
    try {
      tempDir = Files.createTempDirectory("flyway_install_");
    } catch (IOException x) {
      throw new FlywayRuntimeException(x);
    }

    File renamedFile = new File(tempDir.toFile(), "V2__post_create.sql");

    try {
      copyClasspathResourceToFile(classpathResource, renamedFile);
      executeFlyway(
          databaseType,
          "filesystem:" + tempDir.toAbsolutePath(),
          connection,
          classpathResource,
          "flyway_schema_history_install");
    } finally {
      deleteDirectory(tempDir.toFile());
    }
  }

  public void executeMigrationScripts(
      DatabaseType databaseType, String locator, Connection connection) {
    executeFlyway(
        databaseType,
        normalizeLocation(locator),
        connection,
        "migration",
        "flyway_schema_history_migration");
  }

  private void executeFlyway(
      DatabaseType databaseType,
      String location,
      Connection connection,
      String logName,
      String historyTable) {
    int logId = flywayDatabaseUpgradeLog.start(databaseType, connection, logName);

    try {
      Flyway flyway = buildFlyway(location, connection, historyTable);
      flyway.migrate();
      flywayDatabaseUpgradeLog.finish(connection, logId, null);
    } catch (Exception x) {
      flywayDatabaseUpgradeLog.finish(connection, logId, getStackTrace(x));
      throw new FlywayRuntimeException(x);
    }
  }

  private Flyway buildFlyway(String location, Connection connection, String historyTable) {
    return Flyway.configure()
        .dataSource(new NonClosingConnectionDataSource(connection))
        .locations(location)
        .table(historyTable)
        .validateOnMigrate(false)
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .load();
  }

  private String normalizeLocation(String locator) {
    return locator.contains(":") ? locator : "classpath:" + locator;
  }

  public List<String> getPendingMigrations(
      DatabaseType databaseType, String locator, Connection connection) {
    try {
      Flyway flyway =
          buildFlyway(normalizeLocation(locator), connection, "flyway_schema_history_migration");
      return Arrays.stream(flyway.info().pending()).map(MigrationInfo::getScript).toList();
    } catch (Exception x) {
      throw new FlywayRuntimeException(x);
    }
  }

  private void copyClasspathResourceToFile(String classpathResource, File targetFile)
      throws FlywayRuntimeException {
    try (InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream(classpathResource);
        FileOutputStream outputStream = new FileOutputStream(targetFile)) {
      if (inputStream == null) {
        throw new IOException("Classpath resource not found: " + classpathResource);
      }

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    } catch (IOException x) {
      throw new FlywayRuntimeException(x);
    }
  }

  private static String getStackTrace(Throwable x) {
    StringWriter sw = new StringWriter();
    x.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  private void deleteDirectory(File directory) {
    if (!directory.exists()) {
      return;
    }

    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          deleteDirectory(file);
        } else {
          file.delete();
        }
      }
    }

    directory.delete();
  }
}
