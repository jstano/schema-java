package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.IndexGenerator;
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
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQLIndexGenerator")
class PostgreSQLIndexGeneratorTest {

  @Test
  @DisplayName("renders a unique filtered index with WHERE after INCLUDE")
  void rendersUniqueFilteredIndexWithWhereAfterInclude() {
    Table table = new Table(null, "public", "ParentTable", null, null, false);
    table.getColumns().add(new Column("ParentId", ColumnType.INT, 0, false));

    Key key =
        new Key(
            KeyType.INDEX,
            List.of(new KeyColumn("ParentId")),
            false,
            false,
            true,
            "ParentId",
            "ParentId is not null");
    table.getIndexes().add(key);

    String sql = generateIndexesSql(table);

    assertTrue(
        sql.contains(
            "create unique index ix_parenttable1 on public.ParentTable (ParentId) "
                + "include (ParentId) where ParentId is not null;"),
        sql);
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
                DatabaseType.POSTGRESQL,
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

    IndexGenerator gen = new PostgreSQLIndexGenerator(sqlGen);
    gen.outputIndexes(table);
    return sw.toString();
  }
}
