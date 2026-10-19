package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Table;
import com.stano.schema.model.TriggerType;
import java.util.List;

public abstract class TriggerGenerator extends BaseGenerator {
  protected TriggerGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public void outputTriggers() {
    for (Table table : schema.getTables()) {
      outputTriggersForTable(table);
    }
  }

  /**
   * Outputs the delete and update triggers for a single table, as {@link #outputTriggers()} does
   * for every table. The generated SQL is idempotent (create-or-replace / drop-then-create), so a
   * migration can call this to regenerate a table's full trigger set.
   *
   * @param table the table to output triggers for
   */
  public void outputTriggersForTable(Table table) {
    if (shouldOutputDeleteTrigger(table)) {
      outputDeleteTrigger(table, table.getReverseRelations());
    }

    if (shouldOutputUpdateTrigger(table)) {
      outputUpdateTrigger(table, table.getRelations());
    }
  }

  /**
   * Drops the delete and/or update trigger of a table that no longer needs one. The drops are
   * idempotent, so it is safe when the trigger never existed.
   *
   * @param table the table whose obsolete triggers should be dropped
   */
  public void outputDropObsoleteTriggersForTable(Table table) {
    if (!shouldOutputDeleteTrigger(table)) {
      outputDropDeleteTrigger(table);
    }

    if (!shouldOutputUpdateTrigger(table)) {
      outputDropUpdateTrigger(table);
    }
  }

  protected void outputDropDeleteTrigger(Table table) {}

  protected void outputDropUpdateTrigger(Table table) {}

  protected abstract void outputDeleteTrigger(Table table, List<Relation> relations);

  protected abstract void outputUpdateTrigger(Table table, List<Relation> relations);

  private boolean shouldOutputDeleteTrigger(Table table) {
    boolean hasDeleteTriggers =
        table.getTriggers().stream()
            .anyMatch(trigger -> trigger.getTriggerType() == TriggerType.DELETE);

    return hasDeleteTriggers
        || (!table.getReverseRelations().isEmpty()
            && sqlGenerator.getSqlGeneratorOptions().getForeignKeyMode() == ForeignKeyMode.TRIGGERS)
        || !table.getAggregations().isEmpty();
  }

  private boolean shouldOutputUpdateTrigger(Table table) {
    boolean hasUpdateTriggers =
        table.getTriggers().stream()
            .anyMatch(trigger -> trigger.getTriggerType() == TriggerType.UPDATE);

    return hasUpdateTriggers
        || (!table.getRelations().isEmpty()
            && sqlGenerator.getSqlGeneratorOptions().getForeignKeyMode() == ForeignKeyMode.TRIGGERS)
        || !table.getAggregations().isEmpty();
  }
}
