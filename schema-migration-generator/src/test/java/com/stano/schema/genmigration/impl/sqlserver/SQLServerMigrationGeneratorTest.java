package com.stano.schema.genmigration.impl.sqlserver;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Procedure;
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

  @Test
  @DisplayName("generates function SQL for add-function change with SQL Server database type")
  void generatesAddFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddFunctionChange(
            new Function(
                null, "fn_greet", DatabaseType.SQL_SERVER, "CREATE FUNCTION dbo.fn_greet()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE FUNCTION dbo.fn_greet()"));
  }

  @Test
  @DisplayName("generates DROP FUNCTION IF EXISTS dbo for drop-function change")
  void generatesDropFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropFunctionChange("fn_greet", DatabaseType.SQL_SERVER));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP FUNCTION IF EXISTS dbo.fn_greet"));
  }

  @Test
  @DisplayName("generates procedure SQL for add-procedure change with SQL Server database type")
  void generatesAddProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddProcedureChange(
            new Procedure(
                null, "sp_audit", DatabaseType.SQL_SERVER, "CREATE PROCEDURE dbo.sp_audit()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE PROCEDURE dbo.sp_audit()"));
  }

  @Test
  @DisplayName("generates DROP PROCEDURE IF EXISTS dbo for drop-procedure change")
  void generatesDropProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropProcedureChange("sp_audit", DatabaseType.SQL_SERVER));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP PROCEDURE IF EXISTS dbo.sp_audit"));
  }
}
