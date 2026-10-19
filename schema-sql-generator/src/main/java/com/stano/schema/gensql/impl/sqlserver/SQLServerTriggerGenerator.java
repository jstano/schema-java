package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.TriggerGenerator;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Table;
import java.util.List;

class SQLServerTriggerGenerator extends TriggerGenerator {

  private final SQLServerDeleteTriggerGenerator deleteTriggerGenerator;
  private final SQLServerUpdateTriggerGenerator updateTriggerGenerator;

  SQLServerTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);

    this.deleteTriggerGenerator = new SQLServerDeleteTriggerGenerator(sqlGenerator);
    this.updateTriggerGenerator = new SQLServerUpdateTriggerGenerator(sqlGenerator);
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
    String schemaName =
        table.getSchemaName().equalsIgnoreCase("public") ? "dbo" : table.getSchemaName();
    String fullyQualifiedTrigger = schemaName + "." + name;
    sqlWriter.println(
        "if object_id('" + escapeSqlLiteral(fullyQualifiedTrigger) + "', 'TR') is not null");
    sqlWriter.println("   drop trigger " + fullyQualifiedTrigger + statementSeparator);
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
