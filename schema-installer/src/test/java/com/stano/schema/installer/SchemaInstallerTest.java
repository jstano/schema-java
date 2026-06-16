package com.stano.schema.installer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stano.schema.installer.schemacontext.SchemaContext;
import com.stano.schema.model.DatabaseType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaInstaller")
class SchemaInstallerTest {

  private SchemaInstaller installer;
  private Connection conn;

  // Concrete subclass for testing the abstract base
  static class TestSchemaInstaller extends SchemaInstaller {
    @Override
    protected void executeSqlFile(
        Connection connection,
        DatabaseType databaseType,
        SchemaContext schemaContext,
        java.io.File sqlFile) {
      // no-op for test
    }

    @Override
    protected void executePostCreateScript(Connection connection, String postCreateResourceName) {
      // no-op for test
    }
  }

  @BeforeEach
  void setUp() throws SQLException {
    installer = new TestSchemaInstaller();
    conn = DriverManager.getConnection("jdbc:h2:mem:test_" + System.nanoTime());
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
  @DisplayName("getPendingMigrations(Connection) returns empty list when locator is null")
  void getPendingMigrationsReturnsEmptyWhenLocatorNull() throws SQLException {
    SchemaContext context = mock(SchemaContext.class);
    when(context.getMigrationScriptLocator(conn)).thenReturn(null);

    List<String> result = installer.getPendingMigrations(conn, context);

    assertEquals(List.of(), result);
  }

  @Test
  @DisplayName("getPendingMigrations(DataSource) wraps SQLException in SchemaMigrationException")
  void getPendingMigrationsWrapsException() {
    DataSource dataSource = mock(DataSource.class);
    try {
      when(dataSource.getConnection()).thenThrow(new SQLException("test error"));
    } catch (SQLException e) {
      // expected during setup
    }

    SchemaContext context = mock(SchemaContext.class);

    assertThrows(
        SchemaMigrationException.class, () -> installer.getPendingMigrations(dataSource, context));
  }

  @Test
  @DisplayName("getPendingMigrations delegates to findPendingMigrations")
  void getPendingMigrationsDelegatesToFindPendingMigrations() {
    SchemaContext context = mock(SchemaContext.class);
    when(context.getMigrationScriptLocator(conn)).thenReturn("db/migration");

    // The default implementation returns empty list
    List<String> result = installer.getPendingMigrations(conn, context);

    assertEquals(List.of(), result);
  }

  @Test
  @DisplayName("findPendingMigrations returns empty list by default")
  void findPendingMigrationsReturnsEmptyByDefault() {
    List<String> result = installer.findPendingMigrations(conn, DatabaseType.H2, "db/migration");

    assertEquals(List.of(), result);
  }
}
