package com.stano.schema.gensql.impl.h2;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

@DisplayName("H2IndexGenerator")
class H2IndexGeneratorTest {

  @Test
  @DisplayName("ignores the filter predicate (H2 has no partial-index syntax)")
  void ignoresFilterPredicate() {
    Table table = new Table(null, "public", "ParentTable", null, null, false);
    table.getColumns().add(new Column("ParentId", ColumnType.INT, 0, false));

    Key key =
        new Key(
            KeyType.INDEX,
            List.of(new KeyColumn("ParentId")),
            false,
            false,
            true,
            null,
            "ParentId is not null");
    table.getIndexes().add(key);

    String sql = generateIndexesSql(table);

    assertTrue(
        sql.contains("create unique index ix_parenttable1 on public.ParentTable (ParentId)"),
        sql);
    assertFalse(sql.contains("where"), sql);
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
                DatabaseType.H2,
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

    IndexGenerator gen = new H2IndexGenerator(sqlGen);
    gen.outputIndexes(table);
    return sw.toString();
  }
}
