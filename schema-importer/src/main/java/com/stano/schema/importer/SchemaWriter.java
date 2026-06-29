package com.stano.schema.importer;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;

public class SchemaWriter {
  private final PrintWriter out;

  public SchemaWriter(PrintWriter out) {
    this.out = out;
  }

  public void outputSchema(Schema schema) {
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    out.println(
"""
<database xmlns="http://stano.com/database"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://stano.com/database https://raw.githubusercontent.com/jstano/schema-xsd/refs/heads/main/schema.xsd"
          version="1.0">\
""");

    for (int i = 0; i < schema.getTables().size(); i++) {
      var table = schema.getTables().get(i);
      outputTable(table);

      if (i < schema.getTables().size() - 1) {
        out.println();
      }
    }

    out.println("</database>");
  }

  private void outputTable(Table table) {
    out.printf("  <table name=\"%s\">\n", table.getName());

    if (!table.getColumns().isEmpty()) {
      out.printf("    <columns>\n");
      table.getColumns().forEach(this::outputColumn);
      out.printf("    </columns>\n");
    }

    if (!table.getKeys().isEmpty()) {
      out.printf("    <keys>\n");

      if (table.getPrimaryKey() != null) {
        outputPrimaryKey(table.getPrimaryKey());
      }

      outputUniqueKeys(table.getKeys());
      outputIndexKeys(table.getKeys());
      out.printf("    </keys>\n");
    }

    if (!table.getRelations().isEmpty()) {
      out.printf("    <relations>\n");
      table.getRelations().forEach(this::outputRelation);
      out.printf("    </relations>\n");
    }

    if (!table.getConstraints().isEmpty()) {
      outputConstraints(table.getConstraints());
    }

    out.printf("  </table>\n");
  }

  private void outputColumn(Column column) {
    var name = column.getName();
    var columnType = column.getType();
    var length = column.getLength();
    var scale = column.getScale();

    if (columnType == ColumnType.ARRAY) {
      if (column.isRequired()) {
        out.printf(
            "      <column name=\"%s\" type=\"%s\" elementType=\"%s\" required=\"true\"/>\n",
            name,
            columnType.toString().toLowerCase(),
            column.getElementType().toString().toLowerCase());
      } else {
        out.printf(
            "      <column name=\"%s\" type=\"%s\" elementType=\"%s\"/>\n",
            name,
            columnType.toString().toLowerCase(),
            column.getElementType().toString().toLowerCase());
      }
    } else {
      if (column.isRequired()) {
        out.printf(
            "      <column name=\"%s\" type=\"%s\"%s required=\"true\"%s%s/>\n",
            name,
            columnType.toString().toLowerCase(),
            lengthScale(length, scale),
            defaultConstraint(column),
            generatedValue(column));
      } else {
        out.printf(
            "      <column name=\"%s\" type=\"%s\"%s%s%s/>\n",
            name,
            columnType.toString().toLowerCase(),
            lengthScale(length, scale),
            defaultConstraint(column),
            generatedValue(column));
      }
    }
  }

  private String lengthScale(int length, int scale) {
    if (length == 0 && scale == 0) {
      return "";
    }

    if (length > 0 && scale == 0) {
      return String.format(" length=\"%d\"", length);
    }

    return String.format(" length=\"%d\" scale=\"%d\"", length, scale);
  }

  private String defaultConstraint(Column column) {
    String defaultValue = column.getDefaultConstraint();

    if (defaultValue == null
        || (column.getType() == ColumnType.SEQUENCE
            || column.getType() == ColumnType.LONGSEQUENCE)) {
      return "";
    }

    return String.format(" default=\"%s\"", defaultValue);
  }

  private String generatedValue(Column column) {
    String generatedValue = column.getGenerated();

    if (generatedValue == null) {
      return "";
    }

    return String.format(
        " generated=\"%s\"", StringEscapeUtils.escapeXml11(generatedValue.replace("\n", "")));
  }

  private void outputRelation(Relation relation) {
    out.printf(
        "      <relation src=\"%s\" table=\"%s\" column=\"%s\" type=\"%s\"/>\n",
        relation.getFromColumnName(),
        relation.getToTableName(),
        relation.getToColumnName(),
        relation.getType().toString().toLowerCase());
  }

  private void outputPrimaryKey(Key key) {
    out.printf("      <primary>\n");
    outputKeyColumns(key);
    out.printf("      </primary>\n");
  }

  private void outputUniqueKeys(List<Key> keys) {
    var uniqueKeys = keys.stream().filter(key -> key.getType() == KeyType.UNIQUE).toList();

    uniqueKeys.forEach(
        key -> {
          out.printf("      <unique>\n");
          outputKeyColumns(key);
          out.printf("      </unique>\n");
        });
  }

  private void outputIndexKeys(List<Key> keys) {
    var indexKeys = keys.stream().filter(key -> key.getType() == KeyType.INDEX).toList();

    indexKeys.forEach(
        key -> {
          out.printf("      <index>\n");
          outputKeyColumns(key);
          out.printf("      </index>\n");
        });
  }

  private void outputConstraints(List<Constraint> constraints) {
    out.printf("    <constraints>\n");
    constraints.forEach(this::outputConstraint);
    out.printf("    </constraints>\n");
  }

  private void outputConstraint(Constraint constraint) {
    out.printf("      <constraint name=\"%s\">\n", constraint.getName());
    out.printf("        <![CDATA[\n");
    out.printf("        %s\n", constraint.getSql());
    out.printf("        ]]>\n");
    out.printf("      </constraint>\n");
  }

  private void outputKeyColumns(Key key) {
    key.getColumns()
        .forEach(
            keyColumn -> {
              out.printf("        <column name=\"%s\"/>\n", keyColumn.getName());
            });
  }
}
