package com.stano.schema.installer.liquibase;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stano.schema.installer.SchemaInstaller;
import com.stano.schema.model.DatabaseType;
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
@DisplayName("LiquibaseSchemaInstaller")
class LiquibaseSchemaInstallerTest {

  private Connection conn;
  private LiquibaseSchemaInstaller installer;

  @Mock private LiquibaseDatabaseUpdateChecker mockChecker;

  @BeforeEach
  void setUp() throws SQLException {
    conn =
        DriverManager.getConnection("jdbc:h2:mem:test_" + System.nanoTime() + ";MODE=PostgreSQL");
    installer = new LiquibaseSchemaInstaller();
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
  @DisplayName("extends SchemaInstaller")
  void extendsSchemaInstaller() {
    assertInstanceOf(SchemaInstaller.class, installer);
  }

  @Test
  @DisplayName("findPendingMigrations delegates to LiquibaseDatabaseUpdateChecker")
  void findPendingMigrationsDelegatesToLiquibaseDatabaseUpdateChecker() {
    installer.setLiquibaseDatabaseUpdateChecker(mockChecker);
    List<String> expectedPending = List.of("path::1::author", "path::2::author");
    when(mockChecker.getPendingMigrations("db/changelog/db.changelog-master.xml", conn))
        .thenReturn(expectedPending);

    List<String> result =
        installer.findPendingMigrations(
            conn, DatabaseType.H2, "db/changelog/db.changelog-master.xml");

    verify(mockChecker).getPendingMigrations("db/changelog/db.changelog-master.xml", conn);
    assert result.equals(expectedPending);
  }
}
