package com.stano.schema.gendiagram.impl;

import com.stano.schema.gendiagram.DiagramGenerator;
import com.stano.schema.gendiagram.DiagramGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

public class PlantUMLERDiagramGenerator implements DiagramGenerator {
  private final Schema schema;
  private final PrintWriter writer;

  public PlantUMLERDiagramGenerator(DiagramGeneratorOptions options) {
    this.schema = options.getSchema();
    this.writer = options.getWriter();
  }

  @Override
  public void generate() {
    writer.println("@startuml");
    writer.println();

    for (Table table : schema.getTables()) {
      outputTable(table);
    }

    for (Table table : schema.getTables()) {
      outputRelations(table);
    }

    writer.println();
    writer.println("@enduml");
  }

  private void outputTable(Table table) {
    Set<String> pkColumns =
        table.getKeys().stream()
            .filter(k -> k.getType() == KeyType.PRIMARY)
            .flatMap(k -> k.getColumns().stream())
            .map(kc -> kc.getName())
            .collect(Collectors.toSet());

    Set<String> fkColumns =
        table.getRelations().stream()
            .flatMap(r -> r.getColumnPairs().stream())
            .map(ColumnPair::getFromColumnName)
            .collect(Collectors.toSet());

    writer.println("entity " + table.getName() + " {");

    boolean hasPk = table.getColumns().stream().anyMatch(c -> pkColumns.contains(c.getName()));
    boolean separatorWritten = false;

    for (Column column : table.getColumns()) {
      boolean isPk = pkColumns.contains(column.getName());
      boolean isFk = fkColumns.contains(column.getName());

      if (!isPk && hasPk && !separatorWritten) {
        writer.println("  --");
        separatorWritten = true;
      }

      String prefix = isPk ? "  * " : "  ";
      String stereotype = isPk ? " <<PK>>" : isFk ? " <<FK>>" : "";
      writer.println(prefix + column.getName() + " : " + toPlantUMLType(column) + stereotype);
    }

    writer.println("}");
    writer.println();
  }

  private void outputRelations(Table table) {
    for (Relation relation : table.getRelations()) {
      String cardinality = toCardinality(relation.getType());
      String fromColumns =
          relation.getColumnPairs().stream()
              .map(ColumnPair::getFromColumnName)
              .collect(Collectors.joining(", "));
      writer.println(
          relation.getFromTableName()
              + " "
              + cardinality
              + " "
              + relation.getToTableName()
              + " : "
              + fromColumns);
    }
  }

  private String toCardinality(RelationType type) {
    return switch (type) {
      case ENFORCE, CASCADE, DONOTHING -> "}o--||";
      case SETNULL -> "}o--o|";
    };
  }

  private String toPlantUMLType(Column column) {
    return switch (column.getType()) {
      case SEQUENCE, LONGSEQUENCE, BYTE, SHORT, INT -> "INT";
      case LONG -> "BIGINT";
      case FLOAT, DOUBLE -> "FLOAT";
      case DECIMAL -> "DECIMAL";
      case BOOLEAN -> "BOOLEAN";
      case DATE -> "DATE";
      case DATETIME, TIMESTAMP -> "DATETIME";
      case TIME, TIMESTAMPTZ -> "TIME";
      case CHAR -> "CHAR(" + column.getLength() + ")";
      case VARCHAR, ENUM -> "VARCHAR(" + column.getLength() + ")";
      case TEXT, CITEXT, CSTEXT -> "TEXT";
      case BINARY -> "BINARY";
      case UUID -> "UUID";
      case JSON -> "JSON";
      case ARRAY -> "ARRAY";
    };
  }
}
