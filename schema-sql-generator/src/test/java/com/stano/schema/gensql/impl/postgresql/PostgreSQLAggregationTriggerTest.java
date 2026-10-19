package com.stano.schema.gensql.impl.postgresql;

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

@DisplayName("PostgreSQL aggregation triggers")
class PostgreSQLAggregationTriggerTest {

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
    return sw.toString();
  }

  @Test
  @DisplayName("insert/update trigger upserts into the destination keyed on groups and bucket")
  void insertUpdateTriggerUpsertsIntoDestination() {
    String sql = generate(null, AggregationFrequency.MONTHLY);

    assertContains(
        sql, "insert into app.daily_sales as dest (updated_at, sold, store_id, total, sale_count)");
    assertContains(sql, "cast(date_trunc('month', sold) as date) as sold");
    assertContains(sql, "coalesce(amount, 0) as total, 1 as sale_count");
    assertContains(sql, "on conflict (store_id, sold) do update set");
    assertContains(sql, "total = coalesce(dest.total, 0) + excluded.total");
  }

  @Test
  @DisplayName("an update only re-aggregates when a contributing column changed")
  void updateOnlyReaggregatesWhenContributingColumnChanged() {
    String sql = generate(null, AggregationFrequency.DAILY);

    assertContains(sql, "if tg_op = 'INSERT' then");
    assertContains(
        sql,
        "elsif new.amount is distinct from old.amount\n"
            + "         or new.store_ref is distinct from old.store_ref\n"
            + "         or new.sold is distinct from old.sold then");
  }

  @Test
  @DisplayName("criteria filters rows and disables the changed-column shortcut")
  void criteriaFiltersRowsAndDisablesShortcut() {
    String sql = generate("voided = false", AggregationFrequency.DAILY);

    assertContains(sql, "from (select new.*) as src where (voided = false)");
    assertContains(sql, "from (select old.*) as src where (voided = false)");
    assertFalse(sql.contains("is distinct from"), sql);
  }

  @Test
  @DisplayName("delete trigger subtracts the old row from the destination")
  void deleteTriggerSubtractsOldRow() {
    String sql = generate(null, AggregationFrequency.YEARLY);

    assertContains(sql, "create trigger sale_delete after delete on app.sale");
    assertContains(
        sql,
        "update app.daily_sales as dest set\n"
            + "      total = coalesce(dest.total, 0) - agg.total,\n"
            + "      sale_count = coalesce(dest.sale_count, 0) - agg.sale_count,\n"
            + "      updated_at = now()");
    assertContains(sql, "cast(date_trunc('year', sold) as date) as sold");
    assertContains(sql, "where dest.store_id = agg.store_id and dest.sold = agg.sold;");
  }

  private static void assertContains(String sql, String expected) {
    assertTrue(sql.contains(expected), "expected <" + expected + "> in:\n" + sql);
  }
}
