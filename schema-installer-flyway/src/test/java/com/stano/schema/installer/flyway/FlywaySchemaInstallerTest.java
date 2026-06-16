package com.stano.schema.installer.flyway;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;

import com.stano.schema.installer.SchemaInstaller;
import com.stano.schema.model.DatabaseType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlywaySchemaInstaller")
class FlywaySchemaInstallerTest {

  private Connection conn;
  private FlywaySchemaInstaller installer;

  @Mock private FlywayMigrationExecutor mockExecutor;

  @BeforeEach
  void setUp() throws SQLException {
    conn =
        DriverManager.getConnection("jdbc:h2:mem:test_" + System.nanoTime() + ";MODE=PostgreSQL");
    installer = new FlywaySchemaInstaller();
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
  @DisplayName("executeSqlFile delegates to FlywayMigrationExecutor")
  void executeSqlFileDelegatesToFlywayMigrationExecutor() throws IOException {
    installer.setFlywayMigrationExecutor(mockExecutor);

    File tempFile = File.createTempFile("test", ".sql");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("CREATE TABLE dummy (id INTEGER)");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    installer.executeSqlFile(conn, DatabaseType.H2, null, tempFile);

    verify(mockExecutor).executeSqlFile(DatabaseType.H2, tempFile, conn);

    tempFile.delete();
  }

  @Test
  @DisplayName(
      "executePostCreateScript delegates to FlywayMigrationExecutor with derived DatabaseType")
  void executePostCreateScriptDelegatesToFlywayMigrationExecutorWithDerivedDatabaseType() {
    installer.setFlywayMigrationExecutor(mockExecutor);

    installer.executePostCreateScript(conn, "com/example/post-create.sql");

    verify(mockExecutor)
        .executeClasspathSqlLocation(DatabaseType.H2, "com/example/post-create.sql", conn);
  }

  @Test
  @DisplayName("extends SchemaInstaller")
  void extendsSchemaInstaller() {
    assertInstanceOf(SchemaInstaller.class, installer);
  }

  @Test
  @DisplayName("findPendingMigrations delegates to FlywayMigrationExecutor")
  void findPendingMigrationsDelegatesToFlywayMigrationExecutor() {
    installer.setFlywayMigrationExecutor(mockExecutor);
    List<String> expectedPending = List.of("V1__create_test_table.sql", "V2__add_age_column.sql");
    org.mockito.Mockito.when(
            mockExecutor.getPendingMigrations(DatabaseType.H2, "db/migration", conn))
        .thenReturn(expectedPending);

    List<String> result = installer.findPendingMigrations(conn, DatabaseType.H2, "db/migration");

    verify(mockExecutor).getPendingMigrations(DatabaseType.H2, "db/migration", conn);
    assert result.equals(expectedPending);
  }
}
