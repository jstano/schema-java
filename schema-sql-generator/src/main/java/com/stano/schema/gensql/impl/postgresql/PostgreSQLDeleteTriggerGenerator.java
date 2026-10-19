package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.BaseGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;
import java.util.List;
import java.util.stream.Collectors;

public class PostgreSQLDeleteTriggerGenerator extends BaseGenerator {

  public PostgreSQLDeleteTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  public void outputDeleteTrigger(Table table, List<Relation> relations) {

    String deleteFunctionName = table.getName().toLowerCase() + "_delete";
    String deleteTriggerName = table.getName().toLowerCase() + "_delete";

    outputDeleteFunction(table, relations, deleteFunctionName);

    outputDeleteTrigger(table, deleteTriggerName, deleteFunctionName);
  }

  private String getFullyQualifiedFunctionName(Table table, String functionName) {

    return table.getSchemaName() + "." + functionName;
  }

  /** Joins every column pair of a (reverse) relation into an {@code OLD}-matching where clause. */
  private String oldMatchWhereClause(Relation relation) {

    return relation.getColumnPairs().stream()
        .map(pair -> pair.getToColumnName() + " = OLD." + pair.getFromColumnName())
        .collect(Collectors.joining(" and "));
  }

  private void outputDeleteFunction(Table table, List<Relation> relations, String delFunctionName) {

    sqlWriter.println(
        String.format("/* %s */", getFullyQualifiedFunctionName(table, delFunctionName)));
    sqlWriter.println(
        "create or replace function "
            + getFullyQualifiedFunctionName(table, delFunctionName)
            + "() returns trigger as $BODY$");
    sqlWriter.println("begin");

    if (foreignKeyMode == ForeignKeyMode.TRIGGERS) {
      outputDeleteEnforceStatements(relations);

      outputDeleteSetNullStatements(relations);

      outputDeleteCascadeStatements(relations);
    }

    outputDeleteAggregationStatements(table);

    outputDeleteAdditionalTriggerStatements(table);

    sqlWriter.println("   return null;");
    sqlWriter.println("end;");
    sqlWriter.println("$BODY$ language plpgsql" + statementSeparator);
    sqlWriter.println();
  }

  private void outputDeleteTrigger(
      Table table, String deleteTriggerName, String deleteFunctionName) {

    sqlWriter.println(
        "drop trigger if exists "
            + deleteTriggerName
            + " on "
            + getFullyQualifiedTableName(table)
            + " cascade"
            + statementSeparator);
    sqlWriter.println(
        "create trigger "
            + deleteTriggerName
            + " after delete on "
            + getFullyQualifiedTableName(table));
    sqlWriter.println(
        "   for each row execute procedure "
            + getFullyQualifiedFunctionName(table, deleteFunctionName)
            + "()"
            + statementSeparator);
    sqlWriter.println();
  }

  private void outputDeleteEnforceStatements(List<Relation> relations) {

    relations.stream()
        .filter(relation -> relation.getType() == RelationType.ENFORCE)
        .forEach(
            relation -> {
              sqlWriter.println(
                  "   if (select count(*) from "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " where "
                      + oldMatchWhereClause(relation)
                      + ") > 0 then");
              sqlWriter.println(
                  "      raise exception 'The row in "
                      + getFullyQualifiedTableName(schema.getTable(relation.getFromTableName()))
                      + " cannot be deleted. It is being used by a row in the "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " table.';");
              sqlWriter.println("   end if;");
              sqlWriter.println();
            });
  }

  private void outputDeleteSetNullStatements(List<Relation> relations) {

    relations.stream()
        .filter(relation -> relation.getType() == RelationType.SETNULL)
        .forEach(
            relation -> {
              String setClause =
                  relation.getColumnPairs().stream()
                      .map(pair -> pair.getToColumnName() + " = null")
                      .collect(Collectors.joining(", "));
              sqlWriter.println(
                  "   update "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " set "
                      + setClause
                      + " where "
                      + oldMatchWhereClause(relation)
                      + ";");
              sqlWriter.println();
            });
  }

  private void outputDeleteCascadeStatements(List<Relation> relations) {

    relations.stream()
        .filter(relation -> relation.getType() == RelationType.CASCADE)
        .forEach(
            relation -> {
              sqlWriter.println(
                  "   delete from "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " where "
                      + oldMatchWhereClause(relation)
                      + ";");
              sqlWriter.println();
            });
  }

  private void outputDeleteAggregationStatements(Table table) {

    table
        .getAggregations()
        .forEach(
            aggregation -> {
              sqlWriter.println(PostgreSQLAggregationSql.deleteAggregationSql(table, aggregation));
            });
  }

  private void outputDeleteAdditionalTriggerStatements(Table table) {

    if (!table.getTriggers().isEmpty()) {
      for (Trigger trigger : table.getTriggers()) {
        if (trigger.getDatabaseType() == DatabaseType.POSTGRESQL
            && trigger.getTriggerType() == TriggerType.DELETE) {
          sqlWriter.println(trigger.getTriggerText());
        }
      }
    }
  }
}
