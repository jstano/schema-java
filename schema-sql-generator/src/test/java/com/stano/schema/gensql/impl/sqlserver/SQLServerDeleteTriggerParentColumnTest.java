package com.stano.schema.gensql.impl.sqlserver;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
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

@DisplayName("SQL Server delete trigger parent column")
class SQLServerDeleteTriggerParentColumnTest {

  @Test
  @DisplayName("matches child rows on the referenced column, not the parent's first PK column")
  void matchesOnReferencedColumnRatherThanPrimaryKey() {
    Schema schema = new Schema(null);

    Table parent = new Table(null, "app", "parent", null, null, false);
    parent.getColumns().add(new Column("id", ColumnType.INT, 0, true));
    parent.getColumns().add(new Column("code", ColumnType.INT, 0, true));
    parent.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"))));
    parent.getKeys().add(new Key(KeyType.UNIQUE, List.of(new KeyColumn("code"))));
    schema.addTable(parent);

    Table child = new Table(null, "app", "child", null, null, false);
    child.getColumns().add(new Column("parent_code", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(new Relation("child", "parent_code", "parent", "code", RelationType.CASCADE, false));
    schema.addTable(child);
    schema.buildReverseRelations();

    StringWriter sw = new StringWriter();
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
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

    new SQLServerTriggerGenerator(sqlGen).outputTriggers();

    String output = sw.toString();
    assertTrue(
        output.contains("where parent_code in (select code from deleted)"),
        "expected the delete to match on the referenced column, got: " + output);
  }
}
