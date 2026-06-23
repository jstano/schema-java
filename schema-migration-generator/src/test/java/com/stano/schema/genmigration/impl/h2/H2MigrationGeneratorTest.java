package com.stano.schema.genmigration.impl.h2;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
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
        () -> assertTrue(sql.contains("ALTER TABLE customers ALTER COLUMN name RENAME TO full_name")));
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
}
