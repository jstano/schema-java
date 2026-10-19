package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQL delete trigger on a table without a primary key")
class PostgreSQLTriggerGeneratorNoPrimaryKeyTest {

  @Test
  @DisplayName("an explicit delete trigger is still emitted")
  void explicitDeleteTriggerIsEmittedWithoutPrimaryKey() {
    Schema schema = new Schema(null);

    Table log = new Table(null, "app", "log", null, null, false);
    log.getColumns().add(new Column("msg", ColumnType.INT, 0, false));
    log.getTriggers().add(new Trigger("   -- audit", TriggerType.DELETE, DatabaseType.POSTGRESQL));
    schema.addTable(log);
    schema.buildReverseRelations();

    StringWriter sw = new StringWriter();
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
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

    new PostgreSQLTriggerGenerator(sqlGen).outputTriggers();

    String output = sw.toString();
    assertTrue(output.contains("app.log_delete"), "expected the delete trigger, got: " + output);
    assertTrue(output.contains("-- audit"), "expected the trigger text, got: " + output);
  }
}
