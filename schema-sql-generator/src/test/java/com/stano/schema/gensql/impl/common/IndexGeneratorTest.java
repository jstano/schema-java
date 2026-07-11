package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IndexGenerator index name truncation")
class IndexGeneratorTest {

  @Test
  @DisplayName("IX index with single-digit suffix should truncate to maxKeyNameLength")
  void ixWithSingleDigitSuffixShouldTruncateToMax() {
    Table table = new Table(null, "public", "a".repeat(30), null, null, false);
    table.getColumns().add(new Column("col", ColumnType.VARCHAR, 100, false));

    for (int i = 0; i < 9; i++) {
      Key indexKey = new Key(KeyType.INDEX, List.of(new KeyColumn("col")));
      table.getIndexes().add(indexKey);
    }

    String output = generateIndexesSql(table);
    String[] lines = output.split("\n");

    int indexCount = 0;
    for (String line : lines) {
      if (line.contains("create") && line.contains("index")) {
        String ixName = extractIndexName(line);
        assertEquals(
            32,
            ixName.length(),
            "Single-digit IX #" + (indexCount + 1) + " should be exactly 32 chars");
        assertTrue(ixName.startsWith("ix_"));
        assertTrue(ixName.endsWith(String.valueOf(indexCount + 1)));
        indexCount++;
      }
    }
    assertEquals(9, indexCount);
  }

  @Test
  @DisplayName("IX index with double-digit suffix should not exceed maxKeyNameLength")
  void ixWithDoubleDigitSuffixShouldNotExceedMax() {
    Table table = new Table(null, "public", "a".repeat(30), null, null, false);
    table.getColumns().add(new Column("col", ColumnType.VARCHAR, 100, false));

    for (int i = 0; i < 12; i++) {
      Key indexKey = new Key(KeyType.INDEX, List.of(new KeyColumn("col")));
      table.getIndexes().add(indexKey);
    }

    String output = generateIndexesSql(table);
    String[] lines = output.split("\n");

    int indexCount = 0;
    for (String line : lines) {
      if (line.contains("create") && line.contains("index")) {
        String ixName = extractIndexName(line);
        assertTrue(
            ixName.length() <= 32,
            "IX #" + (indexCount + 1) + " should not exceed 32 chars: " + ixName.length());
        assertTrue(ixName.startsWith("ix_"));
        if (indexCount >= 9) {
          assertTrue(
              ixName.endsWith(String.valueOf(indexCount + 1)),
              "Double-digit IX should end with suffix " + (indexCount + 1) + ": " + ixName);
        }
        indexCount++;
      }
    }
    assertEquals(12, indexCount);
  }

  @Test
  @DisplayName("all IX index names should be lowercase")
  void allIxIndexNamesShouldBeLowercase() {
    Table table = new Table(null, "public", "MyLongTableName_" + "a".repeat(20), null, null, false);
    table.getColumns().add(new Column("col", ColumnType.VARCHAR, 100, false));

    for (int i = 0; i < 5; i++) {
      Key indexKey = new Key(KeyType.INDEX, List.of(new KeyColumn("col")));
      table.getIndexes().add(indexKey);
    }

    String output = generateIndexesSql(table);
    String[] lines = output.split("\n");

    for (String line : lines) {
      if (!line.contains("create") || !line.contains("index")) continue;
      String ixName = extractIndexName(line);
      assertEquals(ixName, ixName.toLowerCase(), "Index name should be lowercase: " + ixName);
    }
  }

  private String generateIndexesSql(Table table) {
    Schema schema = new Schema(null);
    schema.addTable(table);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                schema,
                pw,
                DatabaseType.SQL_SERVER,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
                OutputMode.ALL)) {
          @Override
          protected void outputTables() {}

          @Override
          protected void outputRelations() {}

          @Override
          protected void outputIndexes() {}

          @Override
          protected void outputTriggers() {}

          @Override
          protected void outputFunctions() {}

          @Override
          protected void outputViews() {}

          @Override
          protected void outputProcedures() {}

          @Override
          protected void outputOtherSqlTop() {}

          @Override
          protected void outputOtherSqlBottom() {}
        };

    IndexGenerator gen = new IndexGenerator(sqlGen);
    gen.outputIndexes(table);
    return sw.toString();
  }

  private String extractIndexName(String indexSql) {
    Pattern pattern = Pattern.compile("create\\s+(?:unique\\s+)?index\\s+(\\S+)");
    Matcher matcher = pattern.matcher(indexSql);
    if (matcher.find()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException("Could not extract index name from: " + indexSql);
  }
}
