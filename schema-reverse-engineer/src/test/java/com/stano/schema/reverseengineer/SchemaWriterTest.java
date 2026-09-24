package com.stano.schema.reverseengineer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaWriter")
public class SchemaWriterTest {

  @Test
  @DisplayName("outputSchema writes a compositeRelation element for a composite relation")
  void writesCompositeRelation() {
    Schema schema = new Schema(null);
    Table table = new Table(schema, "", "Assignment", null, null, false);
    table
        .getRelations()
        .add(
            Relation.composite(
                "Assignment",
                "Assignment",
                List.of(
                    new ColumnPair("ParentAssignmentID", "ID"),
                    new ColumnPair("PropertyID", "PropertyID")),
                RelationType.CASCADE,
                false));
    schema.addTable(table);

    StringWriter stringWriter = new StringWriter();
    new SchemaWriter(new PrintWriter(stringWriter)).outputSchema(schema);
    String xml = stringWriter.toString();

    assertTrue(xml.contains("<compositeRelation table=\"Assignment\" type=\"cascade\">"));
    assertTrue(xml.contains("<column src=\"ParentAssignmentID\" name=\"ID\"/>"));
    assertTrue(xml.contains("<column src=\"PropertyID\" name=\"PropertyID\"/>"));
    assertTrue(xml.contains("</compositeRelation>"));
    assertFalse(xml.contains("<relation "), "should not fall back to the single-column form");
  }

  @Test
  @DisplayName("outputSchema writes the unchanged <relation> element for a single-column relation")
  void writesSingleColumnRelation() {
    Schema schema = new Schema(null);
    Table table = new Table(schema, "", "child", null, null, false);
    table
        .getRelations()
        .add(new Relation("child", "parent_id", "parent", "id", RelationType.CASCADE, false));
    schema.addTable(table);

    StringWriter stringWriter = new StringWriter();
    new SchemaWriter(new PrintWriter(stringWriter)).outputSchema(schema);
    String xml = stringWriter.toString();

    assertTrue(
        xml.contains(
            "<relation src=\"parent_id\" table=\"parent\" column=\"id\" type=\"cascade\"/>"));
    assertFalse(xml.contains("<compositeRelation"));
  }
}
