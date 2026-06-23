package com.stano.schema.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChangeSetParser")
class ChangeSetParserTest {

  @Test
  @DisplayName("parses add-table element")
  void parsesAddTable() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <add-table name=\"users\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(AddTableChange.class, changeSet.getChanges().get(0));
    assertEquals("users", ((AddTableChange) changeSet.getChanges().get(0)).getTableName());
  }

  @Test
  @DisplayName("parses add-column element")
  void parsesAddColumn() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <add-column table=\"users\" name=\"email\" type=\"varchar\" length=\"255\""
            + " required=\"false\" default=\"test@example.com\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(AddColumnChange.class, changeSet.getChanges().get(0));
    AddColumnChange change = (AddColumnChange) changeSet.getChanges().get(0);
    assertEquals("users", change.getTableName());
    assertEquals("email", change.getColumn().getName());
    assertEquals(255, change.getColumn().getLength());
    assertFalse(change.getColumn().isRequired());
    assertEquals("test@example.com", change.getColumn().getDefaultConstraint());
  }

  @Test
  @DisplayName("parses add-key element")
  void parsesAddKey() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <add-key table=\"users\" type=\"unique\" columns=\"email\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(AddKeyChange.class, changeSet.getChanges().get(0));
    AddKeyChange change = (AddKeyChange) changeSet.getChanges().get(0);
    assertEquals("users", change.getTableName());
    assertEquals("email", change.getKey().getColumnsAsString());
  }

  @Test
  @DisplayName("parses add-relation element")
  void parsesAddRelation() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <add-relation from-table=\"orders\" from-column=\"user_id\""
            + " to-table=\"users\" to-column=\"id\" type=\"cascade\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(AddRelationChange.class, changeSet.getChanges().get(0));
    AddRelationChange change = (AddRelationChange) changeSet.getChanges().get(0);
    assertEquals("orders", change.getRelation().getFromTableName());
    assertEquals("user_id", change.getRelation().getFromColumnName());
    assertEquals("users", change.getRelation().getToTableName());
    assertEquals("id", change.getRelation().getToColumnName());
  }

  @Test
  @DisplayName("parses manual rename-column edit")
  void parsesManualRenameEdit() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <rename-column table=\"users\" old-name=\"name\" new-name=\"full_name\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(RenameColumnChange.class, changeSet.getChanges().get(0));
    RenameColumnChange change = (RenameColumnChange) changeSet.getChanges().get(0);
    assertEquals("users", change.getTableName());
    assertEquals("name", change.getOldName());
    assertEquals("full_name", change.getNewName());
  }

  @Test
  @DisplayName("parses manual rename-table edit")
  void parsesManualRenameTableEdit() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <rename-table old-name=\"customer\" new-name=\"customers\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(RenameTableChange.class, changeSet.getChanges().get(0));
    RenameTableChange change = (RenameTableChange) changeSet.getChanges().get(0);
    assertEquals("customer", change.getOldName());
    assertEquals("customers", change.getNewName());
  }

  @Test
  @DisplayName("parses drop-table element")
  void parsesDropTable() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <drop-table name=\"old_table\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(DropTableChange.class, changeSet.getChanges().get(0));
    assertEquals("old_table", ((DropTableChange) changeSet.getChanges().get(0)).getTableName());
  }

  @Test
  @DisplayName("parses drop-column element")
  void parsesDropColumn() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <drop-column table=\"users\" name=\"legacy_col\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(DropColumnChange.class, changeSet.getChanges().get(0));
    DropColumnChange change = (DropColumnChange) changeSet.getChanges().get(0);
    assertEquals("users", change.getTableName());
    assertEquals("legacy_col", change.getColumnName());
  }

  @Test
  @DisplayName("parses drop-constraint element")
  void parsesDropConstraint() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <drop-constraint table=\"users\" name=\"chk_old\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(DropConstraintChange.class, changeSet.getChanges().get(0));
    DropConstraintChange change = (DropConstraintChange) changeSet.getChanges().get(0);
    assertEquals("users", change.getTableName());
    assertEquals("chk_old", change.getConstraintName());
  }

  @Test
  @DisplayName("parses drop-view element")
  void parsesDropView() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <drop-view name=\"old_view\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(1, changeSet.getChanges().size());
    assertInstanceOf(DropViewChange.class, changeSet.getChanges().get(0));
    assertEquals("old_view", ((DropViewChange) changeSet.getChanges().get(0)).getViewName());
  }

  @Test
  @DisplayName("parses multiple changes in sequence")
  void parsesMultipleChanges() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<changeset>\n"
            + "    <add-table name=\"users\"/>\n"
            + "    <add-table name=\"orders\"/>\n"
            + "    <add-relation from-table=\"orders\" from-column=\"user_id\""
            + " to-table=\"users\" to-column=\"id\" type=\"cascade\"/>\n"
            + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertEquals(3, changeSet.getChanges().size());
    assertInstanceOf(AddTableChange.class, changeSet.getChanges().get(0));
    assertInstanceOf(AddTableChange.class, changeSet.getChanges().get(1));
    assertInstanceOf(AddRelationChange.class, changeSet.getChanges().get(2));
  }

  @Test
  @DisplayName("handles empty changeset")
  void handlesEmptyChangeset() {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<changeset>\n" + "</changeset>";
    InputStream is = new ByteArrayInputStream(xml.getBytes());

    ChangeSetParser parser = new ChangeSetParser();
    ChangeSet changeSet = parser.parse(is);

    assertTrue(changeSet.isEmpty());
  }
}
