package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQL trigger-mode composite foreign key support")
class PostgreSQLTriggerGeneratorCompositeTest {

  @Test
  @DisplayName("delete trigger enforce checks all column pairs joined with and")
  void deleteTriggerEnforceChecksAllColumnPairs() {
    String output = generateTriggers(RelationType.ENFORCE);

    assertTrue(
        output.contains(
            "if (select count(*) from app.child where parent_id = OLD.id and org_id ="
                + " OLD.org_id) > 0"),
        "expected both column pairs ANDed in the enforce where clause, got: " + output);
  }

  @Test
  @DisplayName("delete trigger setnull sets all columns null and matches all column pairs")
  void deleteTriggerSetNullSetsAllColumns() {
    String output = generateTriggers(RelationType.SETNULL);

    assertTrue(
        output.contains(
            "update app.child set parent_id = null, org_id = null where parent_id = OLD.id and"
                + " org_id = OLD.org_id;"),
        "expected all columns set null and matched, got: " + output);
  }

  @Test
  @DisplayName("delete trigger cascade matches all column pairs")
  void deleteTriggerCascadeMatchesAllColumnPairs() {
    String output = generateTriggers(RelationType.CASCADE);

    assertTrue(
        output.contains("delete from app.child where parent_id = OLD.id and org_id = OLD.org_id;"),
        "expected all column pairs ANDed in the cascade where clause, got: " + output);
  }

  @Test
  @DisplayName(
      "update trigger validation guards on all columns not-null and matches all column pairs,"
          + " listing all columns in the error")
  void updateTriggerValidatesAllColumnPairs() {
    String output = generateTriggers(RelationType.ENFORCE);

    assertTrue(
        output.contains("if new.parent_id is not null and new.org_id is not null then"),
        "expected an all-columns not-null guard, got: " + output);
    assertTrue(
        output.contains(
            "if (select count(*) from app.parent where id = new.parent_id and org_id ="
                + " new.org_id) = 0 then"),
        "expected all column pairs ANDed in the match clause, got: " + output);
    assertTrue(
        output.contains("The value of parent_id, org_id was not found in the app.parent table."),
        "expected all columns comma-joined in the error message, got: " + output);
  }

  private String generateTriggers(RelationType relationType) {
    Schema schema = new Schema(null);

    Table parent = new Table(null, "app", "parent", null, null, false);
    parent.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    parent.getColumns().add(new Column("org_id", ColumnType.INT, 0, true));
    parent.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"))));
    schema.addTable(parent);

    Table child = new Table(null, "app", "child", null, null, false);
    child.getColumns().add(new Column("parent_id", ColumnType.INT, 0, false));
    child.getColumns().add(new Column("org_id", ColumnType.INT, 0, false));
    Relation relation =
        Relation.composite(
            "child",
            "parent",
            List.of(new ColumnPair("parent_id", "id"), new ColumnPair("org_id", "org_id")),
            relationType,
            false);
    child.getRelations().add(relation);
    schema.addTable(child);

    schema.buildReverseRelations();

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                schema,
                pw,
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.TRIGGERS,
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

    PostgreSQLTriggerGenerator gen = new PostgreSQLTriggerGenerator(sqlGen);
    gen.outputTriggers();
    return sw.toString();
  }
}
