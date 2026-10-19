package com.stano.schema.reverseengineer;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializes a {@link Schema} model to the vendor-neutral XML schema definition format understood
 * by the rest of the library (the {@code http://stano.com/database} namespace).
 *
 * <p>The generated document contains a {@code <table>} element for each table in the schema,
 * including its columns, primary/unique/index keys, relations, and check constraints, in the order
 * they appear on the {@link Table} model.
 */
public class SchemaWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaWriter.class);

  private final PrintWriter out;

  /**
   * Creates a writer that serializes schemas to the given output.
   *
   * @param out the writer that the generated XML is printed to
   */
  public SchemaWriter(PrintWriter out) {
    this.out = out;
  }

  /**
   * Writes the given schema as a complete XML document, including the XML declaration and the root
   * {@code <database>} element containing one {@code <table>} element per table.
   *
   * @param schema the schema to serialize
   */
  public void outputSchema(Schema schema) {
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    out.println(
"""
<database xmlns="http://stano.com/database"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://stano.com/database http://schema.stano.com/schema.xsd"
          version="1.0">\
""");

    // Tables, functions and procedures in the default schema ("public", or with no schema name
    // recorded) are written flat under <database>; those in any other named schema are grouped and
    // wrapped in a <schema name="..."> element, so the schema name round-trips through the parser
    // instead of being silently discarded.
    Map<String, List<Table>> tablesBySchema = new LinkedHashMap<>();
    Map<String, List<Function>> functionsBySchema = new LinkedHashMap<>();
    Map<String, List<Procedure>> proceduresBySchema = new LinkedHashMap<>();
    for (Table table : schema.getTables()) {
      tablesBySchema
          .computeIfAbsent(schemaKey(table.getSchemaName()), k -> new ArrayList<>())
          .add(table);
    }
    for (Function function : schema.getFunctions()) {
      functionsBySchema
          .computeIfAbsent(schemaKey(function.getSchemaName()), k -> new ArrayList<>())
          .add(function);
    }
    for (Procedure procedure : schema.getProcedures()) {
      proceduresBySchema
          .computeIfAbsent(schemaKey(procedure.getSchemaName()), k -> new ArrayList<>())
          .add(procedure);
    }

    var schemaNames = new LinkedHashSet<String>();
    schemaNames.addAll(tablesBySchema.keySet());
    schemaNames.addAll(functionsBySchema.keySet());
    schemaNames.addAll(proceduresBySchema.keySet());

    boolean first = true;
    for (String schemaName : schemaNames) {
      if (!schemaName.isEmpty()) {
        if (!first) {
          out.println();
        }
        out.printf("  <schema name=\"%s\">\n", escapeAttr(schemaName));
      }

      for (Table table : tablesBySchema.getOrDefault(schemaName, List.of())) {
        if (!first) {
          out.println();
        }
        first = false;
        outputTable(table);
      }
      for (Function function : functionsBySchema.getOrDefault(schemaName, List.of())) {
        if (!first) {
          out.println();
        }
        first = false;
        outputRoutine(
            "function", function.getName(), function.getDatabaseType(), function.getSql());
      }
      for (Procedure procedure : proceduresBySchema.getOrDefault(schemaName, List.of())) {
        if (!first) {
          out.println();
        }
        first = false;
        outputRoutine(
            "procedure", procedure.getName(), procedure.getDatabaseType(), procedure.getSql());
      }

      if (!schemaName.isEmpty()) {
        out.println("  </schema>");
      }
    }

    out.println("</database>");
  }

  private static String schemaKey(String schemaName) {
    return (schemaName == null || schemaName.isBlank() || schemaName.equalsIgnoreCase("public"))
        ? ""
        : schemaName;
  }

  private void outputRoutine(String tag, String name, DatabaseType databaseType, String sql) {
    out.printf("  <%s name=\"%s\">\n", tag, escapeAttr(name));
    out.printf(
        "    <sql databaseType=\"%s\">%s</sql>\n",
        databaseType.toString().toLowerCase(), cdata(sql));
    out.printf("  </%s>\n", tag);
  }

  private void outputTable(Table table) {
    out.printf("  <table name=\"%s\">\n", escapeAttr(table.getName()));

    if (!table.getColumns().isEmpty()) {
      out.printf("    <columns>\n");
      table.getColumns().forEach(this::outputColumn);
      out.printf("    </columns>\n");
    }

    if (!table.getKeys().isEmpty()) {
      // The schema definition requires <primary> whenever <keys> is present, so the unique keys
      // and indexes of a table without a primary key can't be written; skip them with a warning
      // rather than failing the whole run.
      if (table.getPrimaryKey() == null) {
        LOGGER.warn(
            "Table {} has no primary key, so its unique keys and indexes can't be written to the"
                + " schema XML - skipping them",
            table.getName());
      } else {
        out.printf("    <keys>\n");
        outputPrimaryKey(table.getPrimaryKey());
        outputUniqueKeys(table.getKeys());
        outputIndexKeys(table.getKeys());
        out.printf("    </keys>\n");
      }
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
    var name = escapeAttr(column.getName());
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

    return String.format(" default=\"%s\"", escapeAttr(defaultValue));
  }

  private String generatedValue(Column column) {
    String generatedValue = column.getGenerated();

    if (generatedValue == null) {
      return "";
    }

    return String.format(" generated=\"%s\"", escapeAttr(generatedValue));
  }

  private void outputRelation(Relation relation) {
    if (relation.isComposite()) {
      out.printf(
          "      <compositeRelation table=\"%s\" type=\"%s\">\n",
          escapeAttr(relation.getToTableName()), relation.getType().toString().toLowerCase());
      for (var columnPair : relation.getColumnPairs()) {
        out.printf(
            "        <column src=\"%s\" name=\"%s\"/>\n",
            escapeAttr(columnPair.getFromColumnName()), escapeAttr(columnPair.getToColumnName()));
      }
      out.printf("      </compositeRelation>\n");
    } else {
      out.printf(
          "      <relation src=\"%s\" table=\"%s\" column=\"%s\" type=\"%s\"/>\n",
          escapeAttr(relation.getFromColumnName()),
          escapeAttr(relation.getToTableName()),
          escapeAttr(relation.getToColumnName()),
          relation.getType().toString().toLowerCase());
    }
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
          out.printf("      <index%s>\n", indexAttributes(key));
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
    out.printf("      <constraint name=\"%s\">\n", escapeAttr(constraint.getName()));
    out.printf("        %s\n", cdata("\n        " + constraint.getSql() + "\n        "));
    out.printf("      </constraint>\n");
  }

  private void outputKeyColumns(Key key) {
    key.getColumns()
        .forEach(
            keyColumn -> {
              out.printf("        <column name=\"%s\"/>\n", escapeAttr(keyColumn.getName()));
            });
  }

  private static String indexAttributes(Key key) {
    var attributes = new StringBuilder();
    if (key.isUnique()) {
      attributes.append(" unique=\"true\"");
    }
    if (key.getInclude() != null) {
      attributes.append(" include=\"").append(escapeAttr(key.getInclude())).append('"');
    }
    if (key.getFilter() != null) {
      attributes.append(" where=\"").append(escapeAttr(key.getFilter())).append('"');
    }
    return attributes.toString();
  }

  /**
   * Escapes {@code value} for use inside a double-quoted attribute. On top of the XML escapes, the
   * whitespace characters that attribute-value normalization would collapse to a plain space on
   * re-parse (newline, carriage return, tab) are written as character references, so a multi-line
   * default expression round-trips unchanged.
   */
  static String escapeAttr(String value) {
    var escaped = new StringBuilder(value.length());
    for (char ch : StringEscapeUtils.escapeXml11(value).toCharArray()) {
      switch (ch) {
        case '\n' -> escaped.append("&#10;");
        case '\r' -> escaped.append("&#13;");
        case '\t' -> escaped.append("&#9;");
        default -> escaped.append(ch);
      }
    }
    return escaped.toString();
  }

  /** Wraps {@code value} in CDATA, splitting any {@code ]]>} so it can't end the section early. */
  static String cdata(String value) {
    return "<![CDATA[" + value.replace("]]>", "]]]]><![CDATA[>") + "]]>";
  }
}
