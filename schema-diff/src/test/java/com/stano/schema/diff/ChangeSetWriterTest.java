package com.stano.schema.diff;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.View;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChangeSetWriter")
class ChangeSetWriterTest {

  @Test
  @DisplayName("writes add-table element")
  void writesAddTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertTrue(xml.contains("<add-table name=\"users\"/>"));
  }

  @Test
  @DisplayName("writes drop-table element")
  void writesDropTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropTableChange("old_table"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertTrue(xml.contains("<drop-table name=\"old_table\"/>"));
  }

  @Test
  @DisplayName("writes add-column element with attributes")
  void writesAddColumn() {
    ChangeSet changeSet = new ChangeSet();
    Column col =
        new Column(
            "email",
            ColumnType.VARCHAR,
            255,
            0,
            false,
            null,
            "default_email",
            null,
            null,
            null,
            null,
            null);
    changeSet.addChange(new AddColumnChange("users", col));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("table=\"users\"")),
        () -> assertTrue(xml.contains("name=\"email\"")),
        () -> assertTrue(xml.contains("type=\"varchar\"")),
        () -> assertTrue(xml.contains("length=\"255\"")),
        () -> assertTrue(xml.contains("required=\"false\"")),
        () -> assertTrue(xml.contains("default=\"default_email\"")));
  }

  @Test
  @DisplayName("writes drop-column element")
  void writesDropColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropColumnChange("users", "legacy_col"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertTrue(xml.contains("<drop-column table=\"users\" name=\"legacy_col\"/>"));
  }

  @Test
  @DisplayName("writes modify-column element")
  void writesModifyColumn() {
    ChangeSet changeSet = new ChangeSet();
    Column oldCol = new Column("email", ColumnType.VARCHAR, 100, false);
    Column newCol = new Column("email", ColumnType.VARCHAR, 255, true);
    changeSet.addChange(
        new com.stano.schema.diff.change.ModifyColumnChange("users", oldCol, newCol));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("old-type=\"varchar\"")),
        () -> assertTrue(xml.contains("old-length=\"100\"")),
        () -> assertTrue(xml.contains("new-type=\"varchar\"")),
        () -> assertTrue(xml.contains("new-length=\"255\"")));
  }

  @Test
  @DisplayName("writes add-key element")
  void writesAddKey() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("email"));
    Key key = new Key(KeyType.UNIQUE, cols);
    changeSet.addChange(new AddKeyChange("users", key));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("table=\"users\"")),
        () -> assertTrue(xml.contains("type=\"unique\"")),
        () -> assertTrue(xml.contains("columns=\"email\"")));
  }

  @Test
  @DisplayName("writes add-constraint element")
  void writesAddConstraint() {
    ChangeSet changeSet = new ChangeSet();
    Constraint constraint = new Constraint("chk_email", "email like '%@%'", null);
    changeSet.addChange(new AddConstraintChange("users", constraint));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("name=\"chk_email\"")),
        () -> assertTrue(xml.contains("sql=\"email like '%@%'\"")));
  }

  @Test
  @DisplayName("writes add-relation element")
  void writesAddRelation() {
    ChangeSet changeSet = new ChangeSet();
    Relation relation =
        new Relation("orders", "user_id", "users", "id", RelationType.CASCADE, false);
    changeSet.addChange(new AddRelationChange(relation));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("from-table=\"orders\"")),
        () -> assertTrue(xml.contains("from-column=\"user_id\"")),
        () -> assertTrue(xml.contains("to-table=\"users\"")),
        () -> assertTrue(xml.contains("to-column=\"id\"")),
        () -> assertTrue(xml.contains("type=\"cascade\"")));
  }

  @Test
  @DisplayName("writes add-view element")
  void writesAddView() {
    ChangeSet changeSet = new ChangeSet();
    View view = new View(null, "user_summary", "SELECT id, email FROM users", null);
    changeSet.addChange(new AddViewChange(view));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("name=\"user_summary\"")),
        () -> assertTrue(xml.contains("sql=\"SELECT id, email FROM users\"")));
  }

  @Test
  @DisplayName("writes drop-view element")
  void writesDropView() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropViewChange("old_view"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertTrue(xml.contains("<drop-view name=\"old_view\"/>"));
  }

  @Test
  @DisplayName("writes rename-table element")
  void writesRenameTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("customer", "customers"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("old-name=\"customer\"")),
        () -> assertTrue(xml.contains("new-name=\"customers\"")));
  }

  @Test
  @DisplayName("writes rename-column element")
  void writesRenameColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("customers", "name", "full_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("table=\"customers\"")),
        () -> assertTrue(xml.contains("old-name=\"name\"")),
        () -> assertTrue(xml.contains("new-name=\"full_name\"")));
  }

  @Test
  @DisplayName("writes drop-key element")
  void writesDropKey() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("email"));
    Key key = new Key(KeyType.UNIQUE, cols);
    changeSet.addChange(new DropKeyChange("users", key));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("<drop-key")),
        () -> assertTrue(xml.contains("table=\"users\"")),
        () -> assertTrue(xml.contains("type=\"unique\"")),
        () -> assertTrue(xml.contains("columns=\"email\"")));
  }

  @Test
  @DisplayName("writes drop-constraint element")
  void writesDropConstraint() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropConstraintChange("users", "chk_old"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("<drop-constraint")),
        () -> assertTrue(xml.contains("table=\"users\"")),
        () -> assertTrue(xml.contains("name=\"chk_old\"")));
  }

  @Test
  @DisplayName("writes drop-relation element")
  void writesDropRelation() {
    ChangeSet changeSet = new ChangeSet();
    Relation relation =
        new Relation("orders", "user_id", "users", "id", RelationType.CASCADE, false);
    changeSet.addChange(new DropRelationChange(relation));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("<drop-relation")),
        () -> assertTrue(xml.contains("from-table=\"orders\"")),
        () -> assertTrue(xml.contains("from-column=\"user_id\"")),
        () -> assertTrue(xml.contains("to-table=\"users\"")),
        () -> assertTrue(xml.contains("to-column=\"id\"")));
  }

  @Test
  @DisplayName("wraps output with changeset root element")
  void wrapsWithChangesetRoot() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);

    String xml = sw.toString();
    assertAll(
        () -> assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")),
        () -> assertTrue(xml.contains("<changeset>")),
        () -> assertTrue(xml.contains("</changeset>")));
  }
}
