package com.stano.schema.installer.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LiquibaseDatabaseUpdateChecker")
class LiquibaseDatabaseUpdateCheckerTest {

  private Connection conn;
  private LiquibaseDatabaseUpdateChecker checker;

  @BeforeEach
  void setUp() throws Exception {
    conn =
        DriverManager.getConnection("jdbc:h2:mem:test_" + System.nanoTime() + ";MODE=PostgreSQL");
    checker = new LiquibaseDatabaseUpdateChecker();
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
  @DisplayName("getPendingMigrations returns list of pending changesets before any migrations run")
  void getPendingMigrationsReturnsPendingChangeSets() {
    List<String> pending = checker.getPendingMigrations("db/changelog/test-changelog.xml", conn);

    assertEquals(2, pending.size());
    assertTrue(pending.get(0).contains("::1::test"));
    assertTrue(pending.get(1).contains("::2::test"));
  }

  @Test
  @DisplayName("getPendingMigrations returns empty list after migrations run")
  void getPendingMigrationsReturnsEmptyListAfterMigrationsRun() {
    LiquibaseChangeLogExecutor executor = new LiquibaseChangeLogExecutor();
    executor.executeChangeLog("db/changelog/test-changelog.xml", conn);

    List<String> pending = checker.getPendingMigrations("db/changelog/test-changelog.xml", conn);

    assertTrue(pending.isEmpty());
  }
}
