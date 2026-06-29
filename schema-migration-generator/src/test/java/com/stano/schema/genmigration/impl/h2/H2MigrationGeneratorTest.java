package com.stano.schema.genmigration.impl.h2;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Procedure;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("H2MigrationGenerator")
class H2MigrationGeneratorTest {

  @Test
  @DisplayName("generates CREATE TABLE for add-table change")
  void generatesCreateTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
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
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP TABLE IF EXISTS users"));
  }

  @Test
  @DisplayName("generates RENAME TO for rename-table change")
  void generatesRenameTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("customer", "customers"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(() -> assertTrue(sql.contains("ALTER TABLE customer RENAME TO customers")));
  }

  @Test
  @DisplayName("generates ALTER COLUMN RENAME TO for rename-column change")
  void generatesRenameColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("customers", "name", "full_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("ALTER TABLE customers ALTER COLUMN name RENAME TO full_name")));
  }

  @Test
  @DisplayName("drops and re-adds column for modify (H2 limitation)")
  void generatesDropAndReadd() {
    ChangeSet changeSet = new ChangeSet();
    Column oldCol =
        new Column(
            "email", ColumnType.VARCHAR, 100, 0, false, null, null, null, null, null, null, null);
    Column newCol =
        new Column(
            "email", ColumnType.VARCHAR, 255, 0, false, null, null, null, null, null, null, null);
    changeSet.addChange(new ModifyColumnChange("users", oldCol, newCol));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP COLUMN"));
    assertTrue(sql.contains("ADD COLUMN"));
  }

  @Test
  @DisplayName("generates function SQL for add-function change with H2 database type")
  void generatesAddFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddFunctionChange(
            new Function(null, "fn_greet", DatabaseType.H2, "CREATE ALIAS fn_greet AS $$")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE ALIAS fn_greet AS $$"));
  }

  @Test
  @DisplayName("generates DROP FUNCTION IF EXISTS for drop-function change")
  void generatesDropFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropFunctionChange("fn_greet", DatabaseType.H2));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP FUNCTION IF EXISTS fn_greet"));
  }

  @Test
  @DisplayName("generates procedure SQL for add-procedure change with H2 database type")
  void generatesAddProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddProcedureChange(
            new Procedure(null, "sp_audit", DatabaseType.H2, "CREATE PROCEDURE sp_audit()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE PROCEDURE sp_audit()"));
  }

  @Test
  @DisplayName("generates DROP PROCEDURE IF EXISTS for drop-procedure change")
  void generatesDropProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropProcedureChange("sp_audit", DatabaseType.H2));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP PROCEDURE IF EXISTS sp_audit"));
  }
}
