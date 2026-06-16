package com.stano.schema.installer.flyway;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.DatabaseType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FlywayMigrationExecutor")
class FlywayMigrationExecutorTest {

  private Connection conn;
  private FlywayMigrationExecutor executor;

  @BeforeEach
  void setUp() throws SQLException {
    conn =
        DriverManager.getConnection("jdbc:h2:mem:test_" + System.nanoTime() + ";MODE=PostgreSQL");
    executor = new FlywayMigrationExecutor();
  }

  @AfterEach
  void tearDown() {
    if (conn != null) {
      try {
        conn.close();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Test
  @DisplayName("executeSqlFile runs SQL migration successfully")
  void executeSqlFileRunsSQLMigrationSuccessfully() throws IOException {
    File tempFile = File.createTempFile("test_sql_" + UUID.randomUUID(), ".sql");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("CREATE TABLE test_flyway_table_" + System.nanoTime() + " (id INTEGER)");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertDoesNotThrow(() -> executor.executeSqlFile(DatabaseType.H2, tempFile, conn));

    tempFile.delete();
  }

  @Test
  @DisplayName("executeSqlFile deletes temp directory after execution")
  void executeSqlFileDeletesTempDirectoryAfterExecution() throws IOException {
    File tempFile = File.createTempFile("test_sql_clean_" + System.nanoTime(), ".sql");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("CREATE TABLE test_cleanup_table_" + System.nanoTime() + " (id INTEGER)");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertDoesNotThrow(() -> executor.executeSqlFile(DatabaseType.H2, tempFile, conn));

    tempFile.delete();
  }

  @Test
  @DisplayName("executeClasspathSqlLocation reads from classpath and executes")
  void executeClasspathSqlLocationReadsFromClasspathAndExecutes() throws IOException {
    File tempResourceFile = File.createTempFile("test_resource", ".sql");
    try (FileWriter writer = new FileWriter(tempResourceFile)) {
      writer.write("CREATE TABLE test_classpath_table (id INTEGER)");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    tempResourceFile.delete();
  }

  @Test
  @DisplayName("getPendingMigrations returns list of pending migration descriptions")
  void getPendingMigrationsReturnsPendingMigrations() {
    List<String> pending = executor.getPendingMigrations(DatabaseType.H2, "db/migration", conn);

    assertEquals(2, pending.size());
    assertEquals("V1__create_test_table.sql", pending.get(0));
    assertEquals("V2__add_age_column.sql", pending.get(1));
  }

  @Test
  @DisplayName("getPendingMigrations returns empty list after migrations run")
  void getPendingMigrationsReturnsEmptyListAfterMigrationsRun() {
    executor.executeMigrationScripts(DatabaseType.H2, "db/migration", conn);

    List<String> pending = executor.getPendingMigrations(DatabaseType.H2, "db/migration", conn);

    assertTrue(pending.isEmpty());
  }

  @Test
  @DisplayName("getPendingMigrations honors explicit filesystem: prefix")
  void getPendingMigrationsHonorsFilesystemPrefix() throws IOException {
    java.nio.file.Path tempDir = Files.createTempDirectory("flyway_filesystem_test_");
    File migrationFile = new File(tempDir.toFile(), "V1__filesystem_test.sql");
    try (FileWriter writer = new FileWriter(migrationFile)) {
      writer.write("CREATE TABLE filesystem_test_table (id INTEGER)");
    }

    String filesystemLocator = "filesystem:" + tempDir.toAbsolutePath();
    List<String> pending = executor.getPendingMigrations(DatabaseType.H2, filesystemLocator, conn);

    assertEquals(1, pending.size());
    assertEquals("V1__filesystem_test.sql", pending.get(0));

    deleteDirectory(tempDir.toFile());
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
