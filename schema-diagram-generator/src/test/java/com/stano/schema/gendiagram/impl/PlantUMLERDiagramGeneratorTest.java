package com.stano.schema.gendiagram.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gendiagram.DiagramFormat;
import com.stano.schema.gendiagram.DiagramGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.LockEscalation;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlantUMLERDiagramGenerator")
class PlantUMLERDiagramGeneratorTest {

  private Schema buildSchema() {
    return new Schema(null);
  }

  private Table buildTable(Schema schema, String name) {
    Table table = new Table(schema, "dbo", name, null, LockEscalation.AUTO, false);
    schema.addTable(table);
    return table;
  }

  @Test
  @DisplayName("should output @startuml and @enduml wrappers")
  void shouldOutputStartumlAndEndumlWrappers() {
    Schema schema = buildSchema();
    buildTable(schema, "CUSTOMER");

    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    PlantUMLERDiagramGenerator generator =
        new PlantUMLERDiagramGenerator(
            new DiagramGeneratorOptions(schema, writer, DiagramFormat.PLANTUML));

    generator.generate();
    writer.flush();
    String text = output.toString();

    assertTrue(text.contains("@startuml"));
    assertTrue(text.contains("@enduml"));
  }

  @Test
  @DisplayName("should output entity with columns")
  void shouldOutputEntityWithColumns() {
    Schema schema = buildSchema();
    Table table = buildTable(schema, "CUSTOMER");
    table.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    table.getColumns().add(new Column("name", ColumnType.VARCHAR, 255, false));
    table
        .getKeys()
        .add(
            new Key(
                KeyType.PRIMARY,
                new java.util.ArrayList<>(java.util.List.of(new KeyColumn("id")))));

    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    PlantUMLERDiagramGenerator generator =
        new PlantUMLERDiagramGenerator(
            new DiagramGeneratorOptions(schema, writer, DiagramFormat.PLANTUML));

    generator.generate();
    writer.flush();
    String text = output.toString();

    assertTrue(text.contains("entity CUSTOMER {"));
    assertTrue(text.contains("* id : INT <<PK>>"));
    assertTrue(text.contains("--"));
    assertTrue(text.contains("  name : VARCHAR(255)"));
  }

  @Test
  @DisplayName("should annotate FK columns")
  void shouldAnnotateFKColumns() {
    Schema schema = buildSchema();
    Table customer = buildTable(schema, "CUSTOMER");
    customer.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    customer
        .getKeys()
        .add(
            new Key(
                KeyType.PRIMARY,
                new java.util.ArrayList<>(java.util.List.of(new KeyColumn("id")))));

    Table order = buildTable(schema, "ORDER");
    order.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    order.getColumns().add(new Column("customer_id", ColumnType.INT, 0, true));
    order
        .getKeys()
        .add(
            new Key(
                KeyType.PRIMARY,
                new java.util.ArrayList<>(java.util.List.of(new KeyColumn("id")))));
    order
        .getRelations()
        .add(new Relation("ORDER", "customer_id", "CUSTOMER", "id", RelationType.ENFORCE, false));

    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    PlantUMLERDiagramGenerator generator =
        new PlantUMLERDiagramGenerator(
            new DiagramGeneratorOptions(schema, writer, DiagramFormat.PLANTUML));

    generator.generate();
    writer.flush();
    String text = output.toString();

    assertTrue(text.contains("customer_id : INT <<FK>>"));
  }

  @Test
  @DisplayName("should output relationship lines")
  void shouldOutputRelationshipLines() {
    Schema schema = buildSchema();
    Table customer = buildTable(schema, "CUSTOMER");
    customer.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    customer
        .getKeys()
        .add(
            new Key(
                KeyType.PRIMARY,
                new java.util.ArrayList<>(java.util.List.of(new KeyColumn("id")))));

    Table order = buildTable(schema, "ORDER");
    order.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    order.getColumns().add(new Column("customer_id", ColumnType.INT, 0, true));
    order
        .getKeys()
        .add(
            new Key(
                KeyType.PRIMARY,
                new java.util.ArrayList<>(java.util.List.of(new KeyColumn("id")))));
    order
        .getRelations()
        .add(new Relation("ORDER", "customer_id", "CUSTOMER", "id", RelationType.ENFORCE, false));

    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    PlantUMLERDiagramGenerator generator =
        new PlantUMLERDiagramGenerator(
            new DiagramGeneratorOptions(schema, writer, DiagramFormat.PLANTUML));

    generator.generate();
    writer.flush();
    String text = output.toString();

    assertTrue(text.contains("ORDER }o--|| CUSTOMER : customer_id"));
  }

  @Test
  @DisplayName("should list all from-columns for composite relations")
  void compositeRelationListsAllFromColumns() {
    Schema schema = buildSchema();
    Table parent = buildTable(schema, "PARENT");
    parent.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    parent.getColumns().add(new Column("tenant_id", ColumnType.INT, 0, true));
    parent
        .getKeys()
        .add(
            new Key(
                KeyType.PRIMARY,
                new java.util.ArrayList<>(
                    java.util.List.of(new KeyColumn("id"), new KeyColumn("tenant_id")))));

    Table child = buildTable(schema, "CHILD");
    child.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    child.getColumns().add(new Column("parent_id", ColumnType.INT, 0, false));
    child.getColumns().add(new Column("tenant_id", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(
            Relation.composite(
                "CHILD",
                "PARENT",
                List.of(
                    new ColumnPair("parent_id", "id"), new ColumnPair("tenant_id", "tenant_id")),
                RelationType.CASCADE,
                false));

    StringWriter output = new StringWriter();
    PrintWriter writer = new PrintWriter(output);
    PlantUMLERDiagramGenerator generator =
        new PlantUMLERDiagramGenerator(
            new DiagramGeneratorOptions(schema, writer, DiagramFormat.PLANTUML));

    generator.generate();
    writer.flush();
    String text = output.toString();

    assertTrue(text.contains("CHILD }o--|| PARENT : parent_id, tenant_id"), text);
    assertTrue(text.contains("parent_id : INT <<FK>>"), text);
    assertTrue(text.contains("tenant_id : INT <<FK>>"), text);
  }
}
