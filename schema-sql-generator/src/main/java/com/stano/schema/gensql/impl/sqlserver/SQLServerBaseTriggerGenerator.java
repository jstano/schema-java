package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.BaseGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;

class SQLServerBaseTriggerGenerator extends BaseGenerator {

  SQLServerBaseTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  @Override
  protected String getFullyQualifiedTableName(Table table) {

    String schemaName =
        table.getSchemaName().equalsIgnoreCase("public") ? "dbo" : table.getSchemaName();
    if (table.getName().equalsIgnoreCase("function")) {
      return "[" + schemaName + "]" + "." + "[" + table.getName() + "]";
    } else {
      return schemaName + "." + table.getName();
    }
  }

  void outputAdditionalTriggerStatements(Table table, TriggerType triggerType) {

    if (!table.getTriggers().isEmpty()) {
      for (Trigger trigger : table.getTriggers()) {
        if (trigger.getDatabaseType() == DatabaseType.SQL_SERVER
            && trigger.getTriggerType() == triggerType) {
          String triggerText = trigger.getTriggerText();

          if (!triggerText.startsWith("   ")) {
            sqlWriter.print("   ");
          }

          sqlWriter.println(triggerText);
        }
      }
    }
  }
}
