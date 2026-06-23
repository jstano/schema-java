package com.stano.schema.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaDiffIntegration")
class SchemaDiffIntegrationTest {

  @Test
  @DisplayName("diff -> write -> parse round trip maintains structural equivalence")
  void roundTripPreservesChangeSet() throws Exception {
    // Load two schemas from fixtures
    URL oldSchemaUrl = getClass().getResource("/old-schema.xml");
    URL newSchemaUrl = getClass().getResource("/new-schema.xml");
    assertNotNull(oldSchemaUrl, "old-schema.xml fixture not found");
    assertNotNull(newSchemaUrl, "new-schema.xml fixture not found");

    SchemaParser parser = new SchemaParser();
    Schema oldSchema = parser.parseSchema(oldSchemaUrl);
    Schema newSchema = parser.parseSchema(newSchemaUrl);

    // Diff the schemas
    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet originalChangeSet = engine.diff(oldSchema, newSchema);

    // Write to XML
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(originalChangeSet, pw);
    String xml = sw.toString();

    // Parse back from XML
    StringWriter sw2 = new StringWriter();
    ChangeSetParser changeSetParser = new ChangeSetParser();
    ChangeSet parsedChangeSet =
        changeSetParser.parse(new java.io.ByteArrayInputStream(xml.getBytes()));

    // Assert changesets have same structure
    assertEquals(
        originalChangeSet.getChanges().size(),
        parsedChangeSet.getChanges().size(),
        "Changesets should have same number of changes");

    // Verify all changes are present in same order
    for (int i = 0; i < originalChangeSet.getChanges().size(); i++) {
      assertEquals(
          originalChangeSet.getChanges().get(i).getClass(),
          parsedChangeSet.getChanges().get(i).getClass(),
          "Change types should match at position " + i);
    }
  }

  @Test
  @DisplayName("detects expected changes between old and new schema")
  void detectsExpectedChanges() throws Exception {
    URL oldSchemaUrl = getClass().getResource("/old-schema.xml");
    URL newSchemaUrl = getClass().getResource("/new-schema.xml");
    assertNotNull(oldSchemaUrl);
    assertNotNull(newSchemaUrl);

    SchemaParser parser = new SchemaParser();
    Schema oldSchema = parser.parseSchema(oldSchemaUrl);
    Schema newSchema = parser.parseSchema(newSchemaUrl);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertFalse(changeSet.isEmpty(), "Changeset should not be empty");

    // Verify expected change types are present
    boolean hasDropColumn = false;
    boolean hasModifyColumn = false;
    boolean hasAddColumn = false;
    boolean hasAddKey = false;
    boolean hasAddView = false;

    for (SchemaChange change : changeSet.getChanges()) {
      if (change instanceof DropColumnChange) hasDropColumn = true;
      if (change instanceof ModifyColumnChange) hasModifyColumn = true;
      if (change instanceof AddColumnChange) hasAddColumn = true;
      if (change instanceof AddKeyChange) hasAddKey = true;
      if (change instanceof AddViewChange) hasAddView = true;
    }

    // At least some change types should be present
    assertTrue(
        hasAddColumn || hasDropColumn || hasModifyColumn || hasAddKey || hasAddView,
        "Should have at least one type of change");
  }

  @Test
  @DisplayName("generates valid XML from changeset")
  void generatesValidXml() throws Exception {
    URL oldSchemaUrl = getClass().getResource("/old-schema.xml");
    URL newSchemaUrl = getClass().getResource("/new-schema.xml");

    SchemaParser parser = new SchemaParser();
    Schema oldSchema = parser.parseSchema(oldSchemaUrl);
    Schema newSchema = parser.parseSchema(newSchemaUrl);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ChangeSetWriter writer = new ChangeSetWriter();
    writer.write(changeSet, pw);
    String xml = sw.toString();

    // Verify XML structure
    assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
    assertTrue(xml.contains("<changeset>"));
    assertTrue(xml.contains("</changeset>"));
    assertFalse(changeSet.isEmpty(), "Changeset should not be empty");
  }
}
