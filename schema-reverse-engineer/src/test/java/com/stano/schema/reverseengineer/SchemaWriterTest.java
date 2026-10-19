package com.stano.schema.reverseengineer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.parser.SchemaParser;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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

  private static String write(Schema schema) {
    StringWriter stringWriter = new StringWriter();
    new SchemaWriter(new PrintWriter(stringWriter)).outputSchema(schema);
    return stringWriter.toString();
  }

  private static Schema reparse(String xml) {
    return new SchemaParser()
        .parseSchema(null, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  private static Column intColumn(String name, String defaultConstraint) {
    return new Column(
        name, ColumnType.INT, 0, 0, true, null, defaultConstraint, null, null, null, null, null);
  }

  private static Table tableWithPrimaryKey(Schema schema, String name) {
    Table table = new Table(schema, "public", name, null, null, false);
    table.getColumns().add(intColumn("id", null));
    table.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"))));
    return table;
  }

  @Test
  @DisplayName("a multi-line default expression round-trips through the real parser")
  void multiLineDefaultRoundTrips() {
    String defaultExpression = "case\n\twhen a > 0 then 1\r\n  else 0\nend";
    Schema schema = new Schema(null);
    Table table = tableWithPrimaryKey(schema, "t");
    table.getColumns().add(intColumn("a", defaultExpression));
    schema.addTable(table);

    Schema reparsed = reparse(write(schema));

    assertEquals(defaultExpression, reparsed.getTable("t").getColumn("a").getDefaultConstraint());
  }

  @Test
  @DisplayName("special characters in a default expression are escaped, not written raw")
  void specialCharactersInDefaultAreEscaped() {
    Schema schema = new Schema(null);
    Table table = tableWithPrimaryKey(schema, "t");
    table.getColumns().add(intColumn("a", "(x < 1 AND y > \"2\") & 'z'"));
    schema.addTable(table);

    Schema reparsed = reparse(write(schema));

    assertEquals(
        "(x < 1 AND y > \"2\") & 'z'",
        reparsed.getTable("t").getColumn("a").getDefaultConstraint());
  }

  @Test
  @DisplayName("partial and covering indexes keep unique, include and where")
  void partialAndCoveringIndexesRoundTrip() {
    Schema schema = new Schema(null);
    Table table = tableWithPrimaryKey(schema, "t");
    table.getColumns().add(intColumn("a", null));
    table.getColumns().add(intColumn("b", null));
    table
        .getKeys()
        .add(
            new Key(
                KeyType.INDEX,
                List.of(new KeyColumn("a")),
                false,
                false,
                true,
                "b",
                "(b IS NOT NULL AND a > 1)"));
    schema.addTable(table);

    String xml = write(schema);
    Schema reparsed = reparse(xml);

    assertTrue(xml.contains("<index unique=\"true\" include=\"b\""), xml);
    Key index = reparsed.getTable("t").getIndexes().get(0);
    assertTrue(index.isUnique());
    assertEquals("b", index.getInclude());
    assertEquals("(b IS NOT NULL AND a > 1)", index.getFilter());
  }

  @Test
  @DisplayName("a table with no primary key has its unique keys and indexes skipped, not rejected")
  void primaryKeyLessTableSkipsKeys() {
    Schema schema = new Schema(null);
    Table table = new Table(schema, "public", "t", null, null, false);
    table.getColumns().add(intColumn("a", null));
    table.getKeys().add(new Key(KeyType.UNIQUE, List.of(new KeyColumn("a"))));
    table.getKeys().add(new Key(KeyType.INDEX, List.of(new KeyColumn("a"))));
    schema.addTable(table);

    String xml = write(schema);

    assertFalse(xml.contains("<keys>"), xml);
    assertEquals(1, reparse(xml).getTables().size());
  }

  @Test
  @DisplayName("functions and procedures round-trip, including a ]]> inside the body")
  void routinesRoundTrip() {
    Schema schema = new Schema(null);
    schema.addTable(tableWithPrimaryKey(schema, "t"));
    schema.addFunction(
        new Function(
            "public",
            "f1",
            DatabaseType.POSTGRESQL,
            "create or replace function f1() returns int as $$ select arr[1:2]]>1 $$"));
    schema.addProcedure(
        new Procedure(
            "public",
            "p1",
            DatabaseType.POSTGRESQL,
            "create or replace procedure p1() language sql as $$ select 1 $$"));

    Schema reparsed = reparse(write(schema));

    assertEquals(1, reparsed.getFunctions().size());
    assertEquals("f1", reparsed.getFunctions().get(0).getName());
    assertTrue(reparsed.getFunctions().get(0).getSql().contains("arr[1:2]]>1"));
    assertEquals(DatabaseType.POSTGRESQL, reparsed.getFunctions().get(0).getDatabaseType());
    assertEquals("p1", reparsed.getProcedures().get(0).getName());
  }

  @Test
  @DisplayName("a constraint containing ]]> round-trips")
  void constraintWithCdataCloseRoundTrips() {
    Schema schema = new Schema(null);
    Table table = tableWithPrimaryKey(schema, "t");
    table.getConstraints().add(new Constraint("c1", "check (a[1:2]]>0)", null));
    schema.addTable(table);

    Schema reparsed = reparse(write(schema));

    assertTrue(reparsed.getTable("t").getConstraints().get(0).getSql().contains("a[1:2]]>0"));
  }

  @Test
  @DisplayName("a generated column is written as the bare expression")
  void generatedColumnIsBareExpression() {
    Schema schema = new Schema(null);
    Table table = tableWithPrimaryKey(schema, "t");
    table.getColumns().add(intColumn("a", null));
    table
        .getColumns()
        .add(
            new Column(
                "b", ColumnType.INT, 0, 0, false, null, null, "a * 2", null, null, null, null));
    schema.addTable(table);

    Schema reparsed = reparse(write(schema));

    assertEquals("a * 2", reparsed.getTable("t").getColumn("b").getGenerated());
    assertTrue(reparsed.validate().isEmpty(), reparsed.validate().toString());
  }
}
