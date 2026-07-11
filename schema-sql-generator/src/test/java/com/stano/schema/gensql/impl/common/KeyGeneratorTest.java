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

@DisplayName("KeyGenerator constraint name truncation")
class KeyGeneratorTest {

  @Test
  @DisplayName("PK constraint with long table name should truncate to maxKeyNameLength")
  void pkWithLongTableNameShouldTruncateToMax() {
    Table table = new Table(null, "public", "a".repeat(30), null, null, false);
    table.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    Key pkKey = new Key(KeyType.PRIMARY, List.of(new KeyColumn("id")));
    table.getKeys().add(pkKey);

    KeyGenerator gen = createKeyGenerator(table);
    List<String> constraints = gen.getKeyConstraints(table);

    String pkConstraint = constraints.get(0);
    String pkName = extractConstraintName(pkConstraint);

    assertTrue(pkName.length() <= 32);
    assertTrue(pkName.startsWith("pk_"));
  }

  @Test
  @DisplayName("AK constraint with single-digit suffix should truncate to maxKeyNameLength")
  void akWithSingleDigitSuffixShouldTruncateToMax() {
    Table table = new Table(null, "public", "a".repeat(30), null, null, false);
    table.getColumns().add(new Column("col", ColumnType.VARCHAR, 100, false));

    for (int i = 1; i <= 9; i++) {
      Key akKey = new Key(KeyType.UNIQUE, List.of(new KeyColumn("col")));
      table.getKeys().add(akKey);
    }

    KeyGenerator gen = createKeyGenerator(table);
    List<String> constraints = gen.getKeyConstraints(table);

    for (int i = 0; i < constraints.size(); i++) {
      String akName = extractConstraintName(constraints.get(i));
      assertEquals(
          32, akName.length(), "Single-digit AK #" + (i + 1) + " should be exactly 32 chars");
      assertTrue(akName.startsWith("ak_"));
      assertTrue(akName.matches("^ak_.*[1-9]$"), "Name should end with single digit: " + akName);
    }
  }

  @Test
  @DisplayName("AK constraint with double-digit suffix should not exceed maxKeyNameLength")
  void akWithDoubleDigitSuffixShouldNotExceedMax() {
    Table table = new Table(null, "public", "a".repeat(30), null, null, false);
    table.getColumns().add(new Column("col", ColumnType.VARCHAR, 100, false));

    for (int i = 1; i <= 12; i++) {
      Key akKey = new Key(KeyType.UNIQUE, List.of(new KeyColumn("col")));
      table.getKeys().add(akKey);
    }

    KeyGenerator gen = createKeyGenerator(table);
    List<String> constraints = gen.getKeyConstraints(table);

    for (int i = 9; i < constraints.size(); i++) {
      String akName = extractConstraintName(constraints.get(i));
      assertTrue(
          akName.length() <= 32,
          "Double-digit AK #" + (i + 1) + " should not exceed 32 chars: " + akName.length());
      assertTrue(akName.startsWith("ak_"));
      assertTrue(
          akName.endsWith(String.valueOf(i + 1)),
          "Name should end with suffix " + (i + 1) + ": " + akName);
    }
  }

  @Test
  @DisplayName("all AK constraint names should be lowercase")
  void allAkConstraintNamesShouldBeLowercase() {
    Table table = new Table(null, "public", "MyLongTableName_" + "a".repeat(20), null, null, false);
    table.getColumns().add(new Column("col", ColumnType.VARCHAR, 100, false));

    for (int i = 1; i <= 3; i++) {
      Key akKey = new Key(KeyType.UNIQUE, List.of(new KeyColumn("col")));
      table.getKeys().add(akKey);
    }

    KeyGenerator gen = createKeyGenerator(table);
    List<String> constraints = gen.getKeyConstraints(table);

    for (String constraint : constraints) {
      String akName = extractConstraintName(constraint);
      assertEquals(akName, akName.toLowerCase(), "Constraint name should be lowercase: " + akName);
    }
  }

  private KeyGenerator createKeyGenerator(Table table) {
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
    return new KeyGenerator(sqlGen);
  }

  private String extractConstraintName(String constraintSql) {
    Pattern pattern = Pattern.compile("constraint\\s+(\\S+)");
    Matcher matcher = pattern.matcher(constraintSql);
    if (matcher.find()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException("Could not extract constraint name from: " + constraintSql);
  }
}
