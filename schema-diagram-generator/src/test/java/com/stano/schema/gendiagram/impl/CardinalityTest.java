package com.stano.schema.gendiagram.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gendiagram.DiagramFormat;
import com.stano.schema.gendiagram.DiagramGenerator;
import com.stano.schema.gendiagram.DiagramGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.LockEscalation;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("diagram cardinality")
class CardinalityTest {

  private static Schema schemaWith(RelationType type, boolean required) {
    Schema schema = new Schema(null);
    Table parent = new Table(schema, "dbo", "PARENT", null, LockEscalation.AUTO, false);
    parent.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    Table child = new Table(schema, "dbo", "CHILD", null, LockEscalation.AUTO, false);
    child.getColumns().add(new Column("parent_id", ColumnType.INT, 0, required));
    child.getRelations().add(new Relation("CHILD", "parent_id", "PARENT", "id", type, false));
    schema.addTable(parent);
    schema.addTable(child);
    return schema;
  }

  private static String render(
      Function<DiagramGeneratorOptions, DiagramGenerator> factory, Schema schema) {
    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    factory.apply(new DiagramGeneratorOptions(schema, writer, DiagramFormat.MERMAID)).generate();
    writer.flush();
    return output.toString();
  }

  @Test
  @DisplayName("follows the FK column's nullability, not the delete action, in both generators")
  void followsNullabilityNotDeleteAction() {
    record Case(RelationType type, boolean required, String expected) {}
    for (Case c :
        List.of(
            new Case(RelationType.DONOTHING, true, "}o--||"),
            new Case(RelationType.SETNULL, true, "}o--||"),
            new Case(RelationType.CASCADE, false, "}o--o|"),
            new Case(RelationType.ENFORCE, false, "}o--o|"),
            new Case(RelationType.CASCADE, true, "}o--||"))) {
      Schema schema = schemaWith(c.type(), c.required());
      assertTrue(
          render(MermaidERDiagramGenerator::new, schema).contains(c.expected()),
          c.type() + " required=" + c.required() + " (mermaid)");
      assertTrue(
          render(PlantUMLERDiagramGenerator::new, schema).contains(c.expected()),
          c.type() + " required=" + c.required() + " (plantuml)");
    }
  }

  @Test
  @DisplayName("a composite FK is exactly-one only when every local column is required")
  void compositeNeedsEveryColumnRequired() {
    Schema schema = new Schema(null);
    Table child = new Table(schema, "dbo", "CHILD", null, LockEscalation.AUTO, false);
    child.getColumns().add(new Column("a", ColumnType.INT, 0, true));
    child.getColumns().add(new Column("b", ColumnType.INT, 0, false));
    Relation relation =
        Relation.composite(
            "CHILD",
            "PARENT",
            List.of(new ColumnPair("a", "a"), new ColumnPair("b", "b")),
            RelationType.CASCADE,
            false);

    assertEquals("}o--o|", Cardinality.of(child, relation));

    Table orphan = new Table(schema, "dbo", "CHILD", null, LockEscalation.AUTO, false);
    assertEquals("}o--o|", Cardinality.of(orphan, relation), "unknown columns are not required");
  }

  @Test
  @DisplayName("column lookup ignores case")
  void columnLookupIgnoresCase() {
    Schema schema = new Schema(null);
    Table child = new Table(schema, "dbo", "CHILD", null, LockEscalation.AUTO, false);
    child.getColumns().add(new Column("Parent_Id", ColumnType.INT, 0, true));
    Relation relation =
        new Relation("CHILD", "parent_id", "PARENT", "id", RelationType.CASCADE, false);

    assertEquals("}o--||", Cardinality.of(child, relation));
  }

  @Test
  @DisplayName("table and column names are sanitized into diagram identifiers")
  void namesAreSanitized() {
    Schema schema = new Schema(null);
    Table table = new Table(schema, "dbo", "order-items v2", null, LockEscalation.AUTO, false);
    table.getColumns().add(new Column("unit price", ColumnType.INT, 0, true));
    schema.addTable(table);

    String mermaid = render(MermaidERDiagramGenerator::new, schema);
    String plantUml = render(PlantUMLERDiagramGenerator::new, schema);

    assertTrue(mermaid.contains("order_items_v2 {"), mermaid);
    assertTrue(mermaid.contains("int unit_price"), mermaid);
    assertTrue(plantUml.contains("entity order_items_v2 {"), plantUml);
  }
}
