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

public class MermaidERDiagramGenerator implements DiagramGenerator {
  private final Schema schema;
  private final PrintWriter writer;

  public MermaidERDiagramGenerator(DiagramGeneratorOptions options) {
    this.schema = options.getSchema();
    this.writer = options.getWriter();
  }

  @Override
  public void generate() {
    writer.println("erDiagram");

    for (Table table : schema.getTables()) {
      outputTable(table);
    }

    writer.println();

    for (Table table : schema.getTables()) {
      outputRelations(table);
    }
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

    writer.println("  " + table.getName() + " {");
    for (Column column : table.getColumns()) {
      writer.print("    " + toMermaidType(column) + " " + column.getName());
      if (pkColumns.contains(column.getName())) {
        writer.print(" PK");
      }
      if (fkColumns.contains(column.getName())) {
        writer.print(" FK");
      }
      writer.println();
    }
    writer.println("  }");
  }

  private void outputRelations(Table table) {
    for (Relation relation : table.getRelations()) {
      String cardinality = toCardinality(relation.getType());
      String fromColumns =
          relation.getColumnPairs().stream()
              .map(ColumnPair::getFromColumnName)
              .collect(Collectors.joining(", "));
      writer.println(
          "  "
              + relation.getFromTableName()
              + " "
              + cardinality
              + " "
              + relation.getToTableName()
              + " : \""
              + fromColumns
              + "\"");
    }
  }

  private String toCardinality(RelationType type) {
    return switch (type) {
      case ENFORCE, CASCADE, DONOTHING -> "}o--||";
      case SETNULL -> "}o--o|";
    };
  }

  private String toMermaidType(Column column) {
    return switch (column.getType()) {
      case SEQUENCE, LONGSEQUENCE, BYTE, SHORT, INT -> "int";
      case LONG -> "bigint";
      case FLOAT, DOUBLE -> "float";
      case DECIMAL -> "decimal";
      case BOOLEAN -> "boolean";
      case DATE -> "date";
      case DATETIME, TIMESTAMP -> "datetime";
      case TIME, TIMESTAMPTZ -> "time";
      case CHAR, VARCHAR, ENUM -> "string";
      case TEXT, CITEXT, CSTEXT -> "text";
      case BINARY -> "binary";
      case UUID -> "uuid";
      case JSON -> "json";
      case ARRAY -> "string";
    };
  }
}
