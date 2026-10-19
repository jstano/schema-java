package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.TriggerGenerator;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Table;
import java.util.List;

public class PostgreSQLTriggerGenerator extends TriggerGenerator {

  private final PostgreSQLDeleteTriggerGenerator deleteTriggerGenerator;
  private final PostgreSQLUpdateTriggerGenerator updateTriggerGenerator;

  protected PostgreSQLTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);

    deleteTriggerGenerator = new PostgreSQLDeleteTriggerGenerator(sqlGenerator);
    updateTriggerGenerator = new PostgreSQLUpdateTriggerGenerator(sqlGenerator);
  }

  @Override
  protected void outputDropDeleteTrigger(Table table) {

    outputDropTrigger(table, "_delete");
  }

  @Override
  protected void outputDropUpdateTrigger(Table table) {

    outputDropTrigger(table, "_update");
  }

  private void outputDropTrigger(Table table, String suffix) {

    String name = table.getName().toLowerCase() + suffix;
    sqlWriter.println(
        "drop trigger if exists "
            + name
            + " on "
            + getFullyQualifiedTableName(table)
            + " cascade"
            + statementSeparator);
    sqlWriter.println(
        "drop function if exists "
            + table.getSchemaName()
            + "."
            + name
            + "()"
            + statementSeparator);
    sqlWriter.println();
  }

  @Override
  protected void outputDeleteTrigger(Table table, List<Relation> relations) {

    deleteTriggerGenerator.outputDeleteTrigger(table, relations);
  }

  @Override
  protected void outputUpdateTrigger(Table table, List<Relation> relations) {

    updateTriggerGenerator.outputUpdateTrigger(table, relations);
  }
}
