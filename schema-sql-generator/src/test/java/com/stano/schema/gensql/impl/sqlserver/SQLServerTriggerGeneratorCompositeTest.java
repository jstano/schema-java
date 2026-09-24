package com.stano.schema.gensql.impl.sqlserver;

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

@DisplayName("SQL Server trigger-mode composite foreign key support")
class SQLServerTriggerGeneratorCompositeTest {

  @Test
  @DisplayName("delete trigger enforce uses a join-based exists check with a static message")
  void deleteTriggerEnforceUsesJoinBasedExistsCheck() {
    String output = generateTriggers(RelationType.ENFORCE);

    assertTrue(
        output.contains(
            "if exists (select 1 from app.child c inner join deleted d on c.parent_id = d.id and"
                + " c.org_id = d.org_id)"),
        "expected a join-based exists check across both column pairs, got: " + output);
    assertTrue(
        output.contains(
            "select @msg = 'The row in app.parent cannot be deleted. It is being used by a row"
                + " in the app.child table.'"),
        "expected a static composite error message, got: " + output);
  }

  @Test
  @DisplayName("delete trigger setnull uses a join-based update setting all columns null")
  void deleteTriggerSetNullUsesJoinBasedUpdate() {
    String output = generateTriggers(RelationType.SETNULL);

    assertTrue(
        output.contains(
            "update app.child set parent_id = null, org_id = null from app.child inner join"
                + " deleted on app.child.parent_id = deleted.id and app.child.org_id ="
                + " deleted.org_id;"),
        "expected a join-based update setting all columns null, got: " + output);
  }

  @Test
  @DisplayName("delete trigger cascade uses a join-based delete")
  void deleteTriggerCascadeUsesJoinBasedDelete() {
    String output = generateTriggers(RelationType.CASCADE);

    assertTrue(
        output.contains(
            "delete app.child from app.child inner join deleted on app.child.parent_id ="
                + " deleted.id and app.child.org_id = deleted.org_id;"),
        "expected a join-based delete, got: " + output);
  }

  @Test
  @DisplayName(
      "update trigger validation guards on all columns updated/not-null and matches all column"
          + " pairs, listing all columns in the error")
  void updateTriggerValidatesAllColumnPairs() {
    String output = generateTriggers(RelationType.ENFORCE);

    assertTrue(
        output.contains("if update(parent_id) or update(org_id)"),
        "expected an update() check per column, got: " + output);
    assertTrue(
        output.contains(
            "if (select count(*) from Inserted where parent_id is not null and org_id is not"
                + " null) > 0"),
        "expected an all-columns not-null guard, got: " + output);
    assertTrue(
        output.contains(
            "if (select count(*) from app.parent p,Inserted i where p.id = i.parent_id and"
                + " p.org_id = i.org_id) = 0"),
        "expected all column pairs ANDed in the parent-match clause, got: " + output);
    assertTrue(
        output.contains("raiserror ('The parent_id, org_id''s value doesn''t exist in the"),
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
                DatabaseType.SQL_SERVER,
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

    SQLServerTriggerGenerator gen = new SQLServerTriggerGenerator(sqlGen);
    gen.outputTriggers();
    return sw.toString();
  }
}
