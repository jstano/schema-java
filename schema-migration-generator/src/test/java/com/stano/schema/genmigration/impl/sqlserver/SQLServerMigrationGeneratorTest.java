package com.stano.schema.genmigration.impl.sqlserver;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.DatabaseType;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SQLServerMigrationGenerator")
class SQLServerMigrationGeneratorTest {

  @Test
  @DisplayName("generates CREATE TABLE for add-table change")
  void generatesCreateTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("CREATE TABLE users ()"));
  }

  @Test
  @DisplayName("generates DROP TABLE IF EXISTS for drop-table change")
  void generatesDropTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP TABLE IF EXISTS users"));
  }

  @Test
  @DisplayName("generates sp_rename for rename-table change")
  void generatesRenameTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("customer", "customers"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("sp_rename")),
        () -> assertTrue(sql.contains("customer")),
        () -> assertTrue(sql.contains("customers")));
  }

  @Test
  @DisplayName("generates sp_rename with COLUMN for rename-column change")
  void generatesRenameColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("customers", "name", "full_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("sp_rename")),
        () -> assertTrue(sql.contains("customers.name")),
        () -> assertTrue(sql.contains("full_name")),
        () -> assertTrue(sql.contains("'COLUMN'")));
  }

  @Test
  @DisplayName("uses GO statement separator for SQL Server")
  void useGoSeparator() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("GO"));
  }
}
