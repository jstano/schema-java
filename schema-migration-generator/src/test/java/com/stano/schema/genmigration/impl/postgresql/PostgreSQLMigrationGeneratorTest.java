package com.stano.schema.genmigration.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.View;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQLMigrationGenerator")
class PostgreSQLMigrationGeneratorTest {

  @Test
  @DisplayName("generates CREATE TABLE for add-table change")
  void generatesCreateTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
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
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP TABLE IF EXISTS users"));
  }

  @Test
  @DisplayName("generates ADD COLUMN for add-column change")
  void generatesAddColumn() {
    ChangeSet changeSet = new ChangeSet();
    Column col =
        new Column(
            "email", ColumnType.VARCHAR, 255, 0, false, null, null, null, null, null, null, null);
    changeSet.addChange(new AddColumnChange("users", col));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("ALTER TABLE users ADD COLUMN")),
        () -> assertTrue(sql.contains("email")),
        () -> assertTrue(sql.contains("text")));
  }

  @Test
  @DisplayName("generates PRIMARY KEY for add-key change")
  void generatesAddPrimaryKey() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("id"));
    Key key = new Key(KeyType.PRIMARY, cols);
    changeSet.addChange(new AddKeyChange("users", key));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("PRIMARY KEY"));
  }

  @Test
  @DisplayName("generates UNIQUE INDEX for unique key")
  void generatesUniqueIndex() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("email"));
    Key key = new Key(KeyType.UNIQUE, cols);
    changeSet.addChange(new AddKeyChange("users", key));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("CREATE UNIQUE INDEX")),
        () -> assertTrue(sql.contains("users")));
  }

  @Test
  @DisplayName("generates FOREIGN KEY for add-relation change")
  void generatesAddRelation() {
    ChangeSet changeSet = new ChangeSet();
    Relation rel = new Relation("posts", "user_id", "users", "id", RelationType.CASCADE, false);
    changeSet.addChange(new AddRelationChange(rel));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("FOREIGN KEY")),
        () -> assertTrue(sql.contains("ON DELETE CASCADE")));
  }

  @Test
  @DisplayName("generates CREATE VIEW for add-view change")
  void generatesAddView() {
    ChangeSet changeSet = new ChangeSet();
    View view =
        new View(null, "user_posts", "SELECT u.id, p.title FROM users u JOIN posts p", null);
    changeSet.addChange(new AddViewChange(view));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("CREATE OR REPLACE VIEW")),
        () -> assertTrue(sql.contains("user_posts")));
  }

  @Test
  @DisplayName("generates RENAME TO for rename-table change")
  void generatesRenameTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("customer", "customers"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(() -> assertTrue(sql.contains("ALTER TABLE customer RENAME TO customers")));
  }

  @Test
  @DisplayName("generates RENAME COLUMN for rename-column change")
  void generatesRenameColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("customers", "name", "full_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("ALTER TABLE customers RENAME COLUMN name TO full_name")));
  }

  @Test
  @DisplayName("generates function SQL for add-function change with matching database type")
  void generatesAddFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddFunctionChange(
            new Function(
                null,
                "fn_greet",
                DatabaseType.POSTGRESQL,
                "CREATE OR REPLACE FUNCTION fn_greet()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE OR REPLACE FUNCTION fn_greet()"));
  }

  @Test
  @DisplayName("skips add-function change for non-matching database type")
  void skipsAddFunctionForWrongDatabaseType() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddFunctionChange(
            new Function(null, "fn_greet", DatabaseType.SQL_SERVER, "CREATE FUNCTION fn_greet()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().isBlank());
  }

  @Test
  @DisplayName("generates DROP FUNCTION IF EXISTS for drop-function change")
  void generatesDropFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropFunctionChange("fn_greet", DatabaseType.POSTGRESQL));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP FUNCTION IF EXISTS fn_greet CASCADE"));
  }

  @Test
  @DisplayName("generates procedure SQL for add-procedure change with matching database type")
  void generatesAddProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddProcedureChange(
            new Procedure(
                null, "sp_audit", DatabaseType.POSTGRESQL, "CREATE PROCEDURE sp_audit()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE PROCEDURE sp_audit()"));
  }

  @Test
  @DisplayName("generates DROP PROCEDURE IF EXISTS for drop-procedure change")
  void generatesDropProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropProcedureChange("sp_audit", DatabaseType.POSTGRESQL));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP PROCEDURE IF EXISTS sp_audit CASCADE"));
  }

  @Test
  @DisplayName("emits TODO comment for rename candidate before DROP COLUMN")
  void emitsTodoCommentForRenameCandidate() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropColumnChange("users", "first_name", List.of("full_name")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL);
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("-- TODO: possible rename?")),
        () -> assertTrue(sql.contains("RENAME COLUMN first_name TO full_name")),
        () -> assertTrue(sql.contains("ALTER TABLE users DROP COLUMN first_name")));

    int todoIdx = sql.indexOf("-- TODO:");
    int dropIdx = sql.indexOf("ALTER TABLE users DROP COLUMN");
    assertTrue(todoIdx < dropIdx, "TODO comment should appear before DROP COLUMN");
  }

  @Test
  @DisplayName("includes statement separator in output")
  void includesStatementSeparator() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.POSTGRESQL, ";");
    PostgreSQLMigrationGenerator gen = new PostgreSQLMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains(";"));
  }
}
