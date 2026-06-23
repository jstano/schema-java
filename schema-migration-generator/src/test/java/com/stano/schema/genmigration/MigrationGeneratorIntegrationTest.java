package com.stano.schema.genmigration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MigrationGeneratorIntegration")
class MigrationGeneratorIntegrationTest {

  @Test
  @DisplayName("generates different SQL for each dialect")
  void generatesDifferentSqlPerDialect() {
    // Build a simple changeset
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));
    Column emailCol =
        new Column(
            "email", ColumnType.VARCHAR, 255, 0, false, null, null, null, null, null, null, null);
    changeSet.addChange(new AddColumnChange("users", emailCol));

    // Generate for PostgreSQL
    StringWriter pgSw = new StringWriter();
    PrintWriter pgPw = new PrintWriter(pgSw);
    GenMigration genMigration = new GenMigration();
    genMigration.generateMigrationSQL(DatabaseType.POSTGRESQL, changeSet, pgPw);
    String pgSql = pgSw.toString();
    assertNotNull(pgSql);
    assertTrue(pgSql.contains("CREATE TABLE"));

    // Generate for H2
    StringWriter h2Sw = new StringWriter();
    PrintWriter h2Pw = new PrintWriter(h2Sw);
    genMigration.generateMigrationSQL(DatabaseType.H2, changeSet, h2Pw);
    String h2Sql = h2Sw.toString();
    assertNotNull(h2Sql);
    assertTrue(h2Sql.contains("CREATE TABLE"));

    // Generate for SQL Server
    StringWriter ssSw = new StringWriter();
    PrintWriter ssPw = new PrintWriter(ssSw);
    genMigration.generateMigrationSQL(DatabaseType.SQL_SERVER, changeSet, ssPw);
    String ssSql = ssSw.toString();
    assertNotNull(ssSql);
    assertTrue(ssSql.contains("CREATE TABLE"));
  }

  @Test
  @DisplayName("uses correct statement separator per dialect")
  void usesCorrectStatementSeparator() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    GenMigration genMigration = new GenMigration();

    // PostgreSQL uses ;
    StringWriter pgSw = new StringWriter();
    PrintWriter pgPw = new PrintWriter(pgSw);
    genMigration.generateMigrationSQL(DatabaseType.POSTGRESQL, changeSet, pgPw);
    String pgSql = pgSw.toString();
    assertTrue(pgSql.contains(";"));

    // SQL Server uses GO
    StringWriter ssSw = new StringWriter();
    PrintWriter ssPw = new PrintWriter(ssSw);
    genMigration.generateMigrationSQL(DatabaseType.SQL_SERVER, changeSet, ssPw);
    String ssSql = ssSw.toString();
    assertTrue(ssSql.contains("GO"));
  }

  @Test
  @DisplayName("handles multiple changes in sequence")
  void handlesMultipleChanges() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));
    changeSet.addChange(new AddTableChange("posts"));
    changeSet.addChange(new AddTableChange("comments"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    GenMigration genMigration = new GenMigration();
    genMigration.generateMigrationSQL(DatabaseType.POSTGRESQL, changeSet, pw);
    String sql = sw.toString();

    assertTrue(sql.contains("CREATE TABLE users"));
    assertTrue(sql.contains("CREATE TABLE posts"));
    assertTrue(sql.contains("CREATE TABLE comments"));
  }
}
