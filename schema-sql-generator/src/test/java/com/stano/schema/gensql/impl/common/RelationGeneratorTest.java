package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RelationGenerator foreign key name truncation")
class RelationGeneratorTest {

  @Test
  @DisplayName("FK constraint with single-digit suffix should truncate to maxKeyNameLength")
  void fkWithSingleDigitSuffixShouldTruncateToMax() {
    Schema schema = new Schema(null);

    Table sourceTable = new Table(null, "public", "a".repeat(30), null, null, false);
    sourceTable.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    sourceTable.getColumns().add(new Column("target_id", ColumnType.INT, 0, false));
    schema.addTable(sourceTable);

    Table targetTable = new Table(null, "public", "target", null, null, false);
    targetTable.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    schema.addTable(targetTable);

    for (int i = 0; i < 9; i++) {
      Relation relation =
          new Relation("a".repeat(30), "target_id", "target", "id", RelationType.ENFORCE, false);
      sourceTable.getRelations().add(relation);
    }

    String output = generateRelationsSql(schema);
    String[] lines = output.split("\n");

    int relationCount = 0;
    for (String line : lines) {
      if (line.contains("add constraint")) {
        String fkName = extractConstraintName(line);
        assertEquals(
            32,
            fkName.length(),
            "Single-digit FK #" + (relationCount + 1) + " should be exactly 32 chars");
        assertTrue(fkName.startsWith("fk_"));
        relationCount++;
      }
    }
    assertEquals(9, relationCount);
  }

  @Test
  @DisplayName("FK constraint with double-digit suffix should not exceed maxKeyNameLength")
  void fkWithDoubleDigitSuffixShouldNotExceedMax() {
    Schema schema = new Schema(null);

    Table sourceTable = new Table(null, "public", "a".repeat(30), null, null, false);
    sourceTable.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    sourceTable.getColumns().add(new Column("target_id", ColumnType.INT, 0, false));
    schema.addTable(sourceTable);

    Table targetTable = new Table(null, "public", "target", null, null, false);
    targetTable.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    schema.addTable(targetTable);

    for (int i = 0; i < 12; i++) {
      Relation relation =
          new Relation("a".repeat(30), "target_id", "target", "id", RelationType.ENFORCE, false);
      sourceTable.getRelations().add(relation);
    }

    String output = generateRelationsSql(schema);
    String[] lines = output.split("\n");

    int relationCount = 0;
    for (String line : lines) {
      if (line.contains("add constraint")) {
        String fkName = extractConstraintName(line);
        assertTrue(
            fkName.length() <= 32,
            "FK #" + (relationCount + 1) + " should not exceed 32 chars: " + fkName.length());
        assertTrue(fkName.startsWith("fk_"));
        if (relationCount >= 9) {
          assertTrue(
              fkName.endsWith(String.valueOf(relationCount + 1)),
              "Double-digit FK should end with suffix " + (relationCount + 1) + ": " + fkName);
        }
        relationCount++;
      }
    }
    assertEquals(12, relationCount);
  }

  @Test
  @DisplayName("all FK constraint names should be lowercase")
  void allFkConstraintNamesShouldBeLowercase() {
    Schema schema = new Schema(null);

    Table sourceTable =
        new Table(null, "public", "MyLongTableName_" + "a".repeat(20), null, null, false);
    sourceTable.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    sourceTable.getColumns().add(new Column("target_id", ColumnType.INT, 0, false));
    schema.addTable(sourceTable);

    Table targetTable = new Table(null, "public", "target", null, null, false);
    targetTable.getColumns().add(new Column("id", ColumnType.INT, 0, false));
    schema.addTable(targetTable);

    for (int i = 0; i < 3; i++) {
      Relation relation =
          new Relation(
              "MyLongTableName_" + "a".repeat(20),
              "target_id",
              "target",
              "id",
              RelationType.ENFORCE,
              false);
      sourceTable.getRelations().add(relation);
    }

    String output = generateRelationsSql(schema);
    String[] lines = output.split("\n");

    for (String line : lines) {
      if (line.contains("add constraint")) {
        String fkName = extractConstraintName(line);
        assertEquals(
            fkName, fkName.toLowerCase(), "FK constraint name should be lowercase: " + fkName);
      }
    }
  }

  private String generateRelationsSql(Schema schema) {
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

    RelationGenerator gen = new RelationGenerator(sqlGen);
    gen.outputRelations();
    return sw.toString();
  }

  private String extractConstraintName(String constraintSql) {
    Pattern pattern = Pattern.compile("add constraint\\s+(\\S+)");
    Matcher matcher = pattern.matcher(constraintSql);
    if (matcher.find()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException("Could not extract constraint name from: " + constraintSql);
  }
}
