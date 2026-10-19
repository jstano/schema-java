package com.stano.schema.gensql.impl.sqlserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.Aggregation;
import com.stano.schema.model.AggregationColumn;
import com.stano.schema.model.AggregationFrequency;
import com.stano.schema.model.AggregationGroup;
import com.stano.schema.model.AggregationType;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SQLServer aggregation triggers")
class SQLServerAggregationTriggerTest {

  private String generate(String criteria, AggregationFrequency frequency) {
    Aggregation aggregation =
        new Aggregation(
            "daily_sales",
            "sold",
            criteria,
            "updated_at",
            frequency,
            List.of(
                new AggregationColumn(AggregationType.SUM, "amount", "total"),
                new AggregationColumn(AggregationType.COUNT, null, "sale_count")),
            List.of(new AggregationGroup("store", "store_ref", "store_id")));

    Schema schema = new Schema(null);
    Table sale = new Table(null, "app", "sale", null, null, false);
    sale.getColumns().add(new Column("amount", ColumnType.INT, 0, false));
    sale.getAggregations().add(aggregation);
    schema.addTable(sale);
    schema.buildReverseRelations();

    StringWriter sw = new StringWriter();
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
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

    new SQLServerTriggerGenerator(sqlGen).outputTriggers();
    return sw.toString();
  }

  @Test
  @DisplayName("insert/update trigger merges inserted-minus-deleted deltas")
  void insertUpdateTriggerMergesDeltas() {
    String sql = generate(null, AggregationFrequency.MONTHLY);

    assertContains(sql, "cast(dateadd(d, -datepart(d, sold) + 1, sold) as date) as sold");
    assertContains(
        sql,
        "delta as (select total as total, sale_count as sale_count, store_id, sold from I union all"
            + " select -total as total, -sale_count as sale_count, store_id, sold from D)");
    assertContains(
        sql,
        "merge app.daily_sales as target using aggregated as source on"
            + " (target.store_id = source.store_id and target.sold = source.sold)");
    assertContains(sql, "target.total = target.total + source.total");
    assertContains(sql, "target.updated_at = getdate()");
    assertContains(
        sql, "when not matched then insert (updated_at, store_id, sold, total, sale_count)");
  }

  @Test
  @DisplayName("an update only re-aggregates when a contributing column changed")
  void updateOnlyReaggregatesWhenContributingColumnChanged() {
    String sql = generate(null, AggregationFrequency.DAILY);

    assertContains(sql, "if update(amount) or update(store_ref) or update(sold)\n");
  }

  @Test
  @DisplayName("criteria filters both pseudo-tables and disables the update shortcut")
  void criteriaFiltersPseudoTablesAndDisablesShortcut() {
    String sql = generate("voided = 0", AggregationFrequency.WEEKLY);

    assertContains(sql, "from deleted where (voided = 0)");
    assertContains(sql, "from inserted where (voided = 0)");
    assertFalse(sql.contains("if update("), sql);
  }

  @Test
  @DisplayName("delete trigger subtracts the deleted rows")
  void deleteTriggerSubtractsDeletedRows() {
    String sql = generate(null, AggregationFrequency.YEARLY);

    assertContains(sql, "create trigger sale_delete on app.sale for delete as");
    assertContains(
        sql,
        "aggregated as (select sum(total) as total, sum(sale_count) as sale_count, store_id, sold"
            + " from D group by store_id, sold)");
    assertContains(sql, "dest.total = dest.total - aggregated.total");
    assertContains(
        sql,
        "from app.daily_sales as dest join aggregated on dest.store_id = aggregated.store_id"
            + " and dest.sold = aggregated.sold;");
  }

  private static void assertContains(String sql, String expected) {
    assertTrue(sql.contains(expected), "expected <" + expected + "> in:\n" + sql);
  }
}
