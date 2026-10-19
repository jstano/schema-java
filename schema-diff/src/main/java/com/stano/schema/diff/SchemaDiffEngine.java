package com.stano.schema.diff;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddEnumTypeChange;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddInitialDataChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddOtherSqlChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddTriggerChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropEnumTypeChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropInitialDataChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropOtherSqlChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropTriggerChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.diff.change.ModifyEnumTypeChange;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.EnumType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.Function;
import com.stano.schema.model.InitialData;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.OtherSql;
import com.stano.schema.model.OtherSqlOrder;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares two {@link Schema} objects and produces an ordered {@link ChangeSet} describing the
 * structural differences between them.
 *
 * <p>Matching between the old and new schema is done by name: tables, columns, views, and keys are
 * matched by their name (and, for functions/procedures, by name plus {@code DatabaseType}); keys,
 * constraints, and relations are matched structurally (e.g. by column list, SQL text, or from/to
 * table and column) rather than by an identifier. Anything present in {@code oldSchema} but absent
 * from {@code newSchema} (by these matching rules) is reported as a drop; anything present in
 * {@code newSchema} but absent from {@code oldSchema} is reported as an add.
 *
 * <p>When a dropped column has no like-named counterpart in the new table but a same-typed,
 * otherwise-unmatched column exists there, its name is recorded as a rename candidate on the
 * resulting {@code DropColumnChange} (see {@link
 * com.stano.schema.diff.change.DropColumnChange#getRenameCandidates()}) as a hint for callers. This
 * is advisory only: the engine does not itself emit {@code RenameColumnChange} or {@code
 * RenameTableChange} instances — a same-named column/table is always treated as unchanged (or, if
 * its definition differs, as a {@code ModifyColumnChange}), and a removed name paired with an added
 * name is always reported as a separate drop and add rather than collapsed into a rename.
 *
 * <p>Views, functions, and procedures whose SQL text differs between the two schemas are reported
 * as a drop of the old definition followed by an add of the new one, rather than as a modify.
 */
public class SchemaDiffEngine {

  /**
   * Compares {@code oldSchema} against {@code newSchema} and returns the ordered set of changes
   * needed to transform the former into the latter.
   *
   * <p>Changes are emitted in two phases so that dependent objects are removed before their
   * dependencies and created after theirs: a drop phase, in the order views, functions, procedures,
   * relations, keys, constraints, columns, then tables; followed by an add phase, in the order
   * tables, columns, column modifications, keys, constraints, relations, functions, procedures,
   * then views. Enum types, triggers, {@code other_sql} and initial data are also diffed: enum
   * types and {@code other_sql} are added before tables, enum types are dropped last, triggers are
   * added after views, and initial data is added last.
   *
   * @param oldSchema the baseline schema (the "before" state)
   * @param newSchema the target schema (the "after" state)
   * @return an ordered {@link ChangeSet} of the differences, empty if the schemas are equivalent
   */
  public ChangeSet diff(Schema oldSchema, Schema newSchema) {
    ChangeSet changeSet = new ChangeSet();

    // Drop phase: views → functions → procedures → relations → keys → constraints → columns →
    // tables
    dropViews(changeSet, oldSchema, newSchema);
    dropFunctions(changeSet, oldSchema, newSchema);
    dropProcedures(changeSet, oldSchema, newSchema);
    dropRelations(changeSet, oldSchema, newSchema);
    dropKeys(changeSet, oldSchema, newSchema);
    dropConstraints(changeSet, oldSchema, newSchema);
    dropColumns(changeSet, oldSchema, newSchema);
    dropTables(changeSet, oldSchema, newSchema);
    dropTriggers(changeSet, oldSchema, newSchema);
    dropOtherSql(changeSet, oldSchema, newSchema);
    dropInitialData(changeSet, oldSchema, newSchema);

    // Add phase: tables → columns → modify → keys → constraints → relations → functions →
    // procedures → views
    // Enum types and top-of-file other_sql go first - tables and columns may depend on them.
    addEnumTypes(changeSet, oldSchema, newSchema);
    addOtherSql(changeSet, oldSchema, newSchema, OtherSqlOrder.TOP);
    addTables(changeSet, oldSchema, newSchema);
    addColumns(changeSet, oldSchema, newSchema);
    modifyColumns(changeSet, oldSchema, newSchema);
    addKeys(changeSet, oldSchema, newSchema);
    addConstraints(changeSet, oldSchema, newSchema);
    addRelations(changeSet, oldSchema, newSchema);
    addFunctions(changeSet, oldSchema, newSchema);
    addProcedures(changeSet, oldSchema, newSchema);
    addViews(changeSet, oldSchema, newSchema);
    addTriggers(changeSet, oldSchema, newSchema);
    // Last: initial data rows reference columns and keys that must already exist.
    addInitialData(changeSet, oldSchema, newSchema);
    // Bottom-of-file other_sql comes after everything else, as in a fresh install.
    addOtherSql(changeSet, oldSchema, newSchema, OtherSqlOrder.BOTTOM);
    // Enum drops go last of all: Postgres can't DROP TYPE while a column still uses it, and columns
    // that move off a dropped enum are only retyped by modifyColumns in the add phase.
    dropEnumTypes(changeSet, oldSchema, newSchema);

    return changeSet;
  }

  private void dropViews(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, View> newViewsByName = indexViewsByName(newSchema);
    for (View oldView : oldSchema.getViews()) {
      View newView = newViewsByName.get(oldView.getName());
      if (newView == null) {
        changeSet.addChange(new DropViewChange(oldView.getName(), oldView.getDatabaseType()));
      } else if (!viewsEqual(oldView, newView)) {
        // View SQL (or database type) changed: drop the old one so addViews() re-creates it.
        changeSet.addChange(new DropViewChange(oldView.getName(), oldView.getDatabaseType()));
      }
    }
  }

  private void dropRelations(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      Table newTable = newTablesByName.get(oldTable.getName());
      if (newTable == null) {
        continue;
      }

      // Ordinal is the relation's 1-based position in the table's relation list, matching the
      // create path's `fk_<table><n>` numbering, so the migration generator can reproduce the
      // exact name of the constraint it's dropping.
      List<Relation> oldRelations = oldTable.getRelations();
      for (int i = 0; i < oldRelations.size(); i++) {
        Relation oldRelation = oldRelations.get(i);
        if (!containsRelation(newTable.getRelations(), oldRelation)) {
          changeSet.addChange(new DropRelationChange(oldRelation, i + 1));
        }
      }
    }
  }

  private void dropKeys(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      Table newTable = newTablesByName.get(oldTable.getName());
      if (newTable == null) {
        continue;
      }

      // Ordinal counts only siblings of the same category (unique keys among unique keys,
      // indexes among indexes) in list order - matching how the create path numbers
      // `ak_<table><n>`/`ix_<table><n>` - so the migration generator can reproduce the exact
      // same name for the object it's dropping.
      int uniqueOrdinal = 0;
      for (Key oldKey : oldTable.getKeys()) {
        if (oldKey.getType() == KeyType.UNIQUE) {
          uniqueOrdinal++;
        }
        if (!keyExistsIn(oldKey, newTable.getKeys())
            && !keyExistsIn(oldKey, newTable.getIndexes())) {
          changeSet.addChange(new DropKeyChange(oldTable.getName(), oldKey, uniqueOrdinal));
        }
      }

      int indexOrdinal = 0;
      for (Key oldKey : oldTable.getIndexes()) {
        if (oldKey.getType() == KeyType.INDEX) {
          indexOrdinal++;
        }
        if (!keyExistsIn(oldKey, newTable.getKeys())
            && !keyExistsIn(oldKey, newTable.getIndexes())) {
          changeSet.addChange(new DropKeyChange(oldTable.getName(), oldKey, indexOrdinal));
        }
      }
    }
  }

  private void dropConstraints(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      Table newTable = newTablesByName.get(oldTable.getName());
      if (newTable == null) {
        continue;
      }
      for (Constraint oldConstraint : oldTable.getConstraints()) {
        if (!containsConstraint(newTable.getConstraints(), oldConstraint)) {
          changeSet.addChange(
              new DropConstraintChange(
                  oldTable.getName(), oldConstraint.getName(), oldConstraint.getDatabaseType()));
        }
      }
    }
  }

  private void dropColumns(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      Table newTable = newTablesByName.get(oldTable.getName());
      if (newTable == null) {
        continue;
      }
      Map<String, Column> newColumnsByName = indexColumnsByName(newTable);
      Map<String, Column> oldColumnsByName = indexColumnsByName(oldTable);
      for (Column oldColumn : oldTable.getColumns()) {
        if (!newColumnsByName.containsKey(oldColumn.getName())) {
          List<String> candidates = new ArrayList<>();
          for (Column newColumn : newTable.getColumns()) {
            if (!oldColumnsByName.containsKey(newColumn.getName())
                && newColumn.getType() == oldColumn.getType()) {
              candidates.add(newColumn.getName());
            }
          }
          changeSet.addChange(
              new DropColumnChange(oldTable.getName(), oldColumn.getName(), candidates));
        }
      }
    }
  }

  private void dropTables(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      if (!newTablesByName.containsKey(oldTable.getName())) {
        changeSet.addChange(new DropTableChange(oldTable.getName()));
      }
    }
  }

  private void addTables(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      if (!oldTablesByName.containsKey(newTable.getName())) {
        changeSet.addChange(new AddTableChange(newTable.getName()));
      }
    }
  }

  private void addColumns(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());
      if (oldTable == null) {
        // New table, all columns will be added via AddColumnChange
        for (Column newColumn : newTable.getColumns()) {
          changeSet.addChange(new AddColumnChange(newTable.getName(), newColumn));
        }
        continue;
      }
      Map<String, Column> oldColumnsByName = indexColumnsByName(oldTable);
      for (Column newColumn : newTable.getColumns()) {
        if (!oldColumnsByName.containsKey(newColumn.getName())) {
          changeSet.addChange(new AddColumnChange(newTable.getName(), newColumn));
        }
      }
    }
  }

  private void modifyColumns(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());
      if (oldTable == null) {
        continue;
      }
      Map<String, Column> oldColumnsByName = indexColumnsByName(oldTable);
      for (Column newColumn : newTable.getColumns()) {
        Column oldColumn = oldColumnsByName.get(newColumn.getName());
        if (oldColumn != null && !columnsEqual(oldColumn, newColumn)) {
          changeSet.addChange(new ModifyColumnChange(newTable.getName(), oldColumn, newColumn));
        }
      }
    }
  }

  private void addKeys(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());
      if (oldTable == null) {
        // New table, all keys will be added
        int uniqueOrdinal = 0;
        for (Key newKey : newTable.getKeys()) {
          if (newKey.getType() == KeyType.UNIQUE) {
            uniqueOrdinal++;
          }
          changeSet.addChange(new AddKeyChange(newTable.getName(), newKey, uniqueOrdinal));
        }
        int indexOrdinal = 0;
        for (Key newKey : newTable.getIndexes()) {
          if (newKey.getType() == KeyType.INDEX) {
            indexOrdinal++;
          }
          changeSet.addChange(new AddKeyChange(newTable.getName(), newKey, indexOrdinal));
        }
        continue;
      }

      // See dropKeys - same per-category ordinal so an added key gets the name the create path
      // would give it.
      int uniqueOrdinal = 0;
      for (Key newKey : newTable.getKeys()) {
        if (newKey.getType() == KeyType.UNIQUE) {
          uniqueOrdinal++;
        }
        if (!keyExistsIn(newKey, oldTable.getKeys())
            && !keyExistsIn(newKey, oldTable.getIndexes())) {
          changeSet.addChange(new AddKeyChange(newTable.getName(), newKey, uniqueOrdinal));
        }
      }

      int indexOrdinal = 0;
      for (Key newKey : newTable.getIndexes()) {
        if (newKey.getType() == KeyType.INDEX) {
          indexOrdinal++;
        }
        if (!keyExistsIn(newKey, oldTable.getKeys())
            && !keyExistsIn(newKey, oldTable.getIndexes())) {
          changeSet.addChange(new AddKeyChange(newTable.getName(), newKey, indexOrdinal));
        }
      }
    }
  }

  private void addConstraints(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());
      if (oldTable == null) {
        // New table, all constraints will be added
        for (Constraint newConstraint : newTable.getConstraints()) {
          changeSet.addChange(new AddConstraintChange(newTable.getName(), newConstraint));
        }
        continue;
      }
      for (Constraint newConstraint : newTable.getConstraints()) {
        if (!containsConstraint(oldTable.getConstraints(), newConstraint)) {
          changeSet.addChange(new AddConstraintChange(newTable.getName(), newConstraint));
        }
      }
    }
  }

  private void addRelations(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());

      // A brand-new table (no oldTable) still needs its relations emitted - addTables() only
      // creates an empty stub table, and the column/key changes that follow build it up via
      // separate ALTER TABLE statements that don't touch relations - so compare against an
      // empty list rather than skipping the table entirely.
      List<Relation> oldRelations = oldTable == null ? List.of() : oldTable.getRelations();

      // See dropRelations - same ordinal so an added relation gets the name the create path
      // would give it.
      List<Relation> newRelations = newTable.getRelations();
      for (int i = 0; i < newRelations.size(); i++) {
        Relation newRelation = newRelations.get(i);
        if (!containsRelation(oldRelations, newRelation)) {
          changeSet.addChange(new AddRelationChange(newRelation, i + 1));
        }
      }
    }
  }

  private void addViews(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, View> oldViewsByName = indexViewsByName(oldSchema);
    for (View newView : newSchema.getViews()) {
      View oldView = oldViewsByName.get(newView.getName());
      if (oldView == null) {
        changeSet.addChange(new AddViewChange(newView));
      } else if (!viewsEqual(oldView, newView)) {
        // dropViews already emitted a DropViewChange; emit the AddViewChange with new SQL.
        changeSet.addChange(new AddViewChange(newView));
      }
    }
  }

  private void dropFunctions(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Function> newByKey = indexFunctionsByKey(newSchema);
    for (Function old : oldSchema.getFunctions()) {
      Function nw = newByKey.get(functionKey(old));
      if (nw == null || !java.util.Objects.equals(old.getSql(), nw.getSql())) {
        changeSet.addChange(new DropFunctionChange(old.getName(), old.getDatabaseType()));
      }
    }
  }

  private void addFunctions(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Function> oldByKey = indexFunctionsByKey(oldSchema);
    for (Function nw : newSchema.getFunctions()) {
      Function old = oldByKey.get(functionKey(nw));
      if (old == null) {
        changeSet.addChange(new AddFunctionChange(nw));
      } else if (!java.util.Objects.equals(old.getSql(), nw.getSql())) {
        changeSet.addChange(new AddFunctionChange(nw));
      }
    }
  }

  private void dropProcedures(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Procedure> newByKey = indexProceduresByKey(newSchema);
    for (Procedure old : oldSchema.getProcedures()) {
      Procedure nw = newByKey.get(procedureKey(old));
      if (nw == null || !java.util.Objects.equals(old.getSql(), nw.getSql())) {
        changeSet.addChange(new DropProcedureChange(old.getName(), old.getDatabaseType()));
      }
    }
  }

  private void addProcedures(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Procedure> oldByKey = indexProceduresByKey(oldSchema);
    for (Procedure nw : newSchema.getProcedures()) {
      Procedure old = oldByKey.get(procedureKey(nw));
      if (old == null) {
        changeSet.addChange(new AddProcedureChange(nw));
      } else if (!java.util.Objects.equals(old.getSql(), nw.getSql())) {
        changeSet.addChange(new AddProcedureChange(nw));
      }
    }
  }

  private void addEnumTypes(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, EnumType> oldByName = indexEnumTypesByName(oldSchema);
    for (EnumType nw : newSchema.getEnumTypes()) {
      EnumType old = oldByName.get(nw.getName().toLowerCase());
      if (old == null) {
        changeSet.addChange(new AddEnumTypeChange(nw));
      } else if (!enumTypesEqual(old, nw)) {
        changeSet.addChange(new ModifyEnumTypeChange(old, nw));
      }
    }
  }

  private void dropEnumTypes(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, EnumType> newByName = indexEnumTypesByName(newSchema);
    for (EnumType old : oldSchema.getEnumTypes()) {
      if (!newByName.containsKey(old.getName().toLowerCase())) {
        changeSet.addChange(new DropEnumTypeChange(old.getName()));
      }
    }
  }

  private Map<String, EnumType> indexEnumTypesByName(Schema schema) {
    Map<String, EnumType> map = new HashMap<>();
    for (EnumType enumType : schema.getEnumTypes()) {
      map.put(enumType.getName().toLowerCase(), enumType);
    }
    return map;
  }

  private boolean enumTypesEqual(EnumType a, EnumType b) {
    List<EnumValue> av = a.getValues();
    List<EnumValue> bv = b.getValues();
    if (av.size() != bv.size()) {
      return false;
    }
    for (int i = 0; i < av.size(); i++) {
      if (!java.util.Objects.equals(av.get(i).getName(), bv.get(i).getName())
          || !java.util.Objects.equals(av.get(i).getCode(), bv.get(i).getCode())) {
        return false;
      }
    }
    return true;
  }

  private void addOtherSql(
      ChangeSet changeSet, Schema oldSchema, Schema newSchema, OtherSqlOrder order) {
    for (OtherSql nw : newSchema.getOtherSql()) {
      // Blank entries generate nothing on a fresh install, so a migration must not emit them
      // either.
      if (nw.getOrder() == order
          && !trimmed(nw.getSql()).isEmpty()
          && !containsOtherSql(oldSchema.getOtherSql(), nw)) {
        changeSet.addChange(new AddOtherSqlChange(nw));
      }
    }
  }

  private void dropOtherSql(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    for (OtherSql old : oldSchema.getOtherSql()) {
      if (!trimmed(old.getSql()).isEmpty() && !containsOtherSql(newSchema.getOtherSql(), old)) {
        changeSet.addChange(new DropOtherSqlChange(old));
      }
    }
  }

  private boolean containsOtherSql(List<OtherSql> entries, OtherSql target) {
    return entries.stream()
        .anyMatch(
            o ->
                o.getDatabaseType() == target.getDatabaseType()
                    && o.getOrder() == target.getOrder()
                    && trimmed(o.getSql()).equals(trimmed(target.getSql())));
  }

  // Triggers are always a separate generation pass, even for a brand-new table, so a new table's
  // triggers are diffed against an empty list instead of skipping the table.
  private void addTriggers(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());
      List<Trigger> oldTriggers = oldTable == null ? List.of() : oldTable.getTriggers();
      for (Trigger trigger : newTable.getTriggers()) {
        if (!containsTrigger(oldTriggers, trigger)) {
          changeSet.addChange(new AddTriggerChange(newTable.getName(), trigger));
        }
      }
    }
  }

  private void dropTriggers(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      Table newTable = newTablesByName.get(oldTable.getName());
      if (newTable == null) {
        continue;
      }
      for (Trigger trigger : oldTable.getTriggers()) {
        if (!containsTrigger(newTable.getTriggers(), trigger)) {
          changeSet.addChange(new DropTriggerChange(oldTable.getName(), trigger));
        }
      }
    }
  }

  private boolean containsTrigger(List<Trigger> triggers, Trigger target) {
    return triggers.stream()
        .anyMatch(
            t ->
                t.getTriggerType() == target.getTriggerType()
                    && t.getDatabaseType() == target.getDatabaseType()
                    && trimmed(t.getTriggerText()).equals(trimmed(target.getTriggerText())));
  }

  // Java's AddTableChange only creates an empty table that columns are then added to, so nothing
  // else emits a new table's initial data: it is diffed against an empty list.
  private void addInitialData(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> oldTablesByName = indexTablesByName(oldSchema);
    for (Table newTable : newSchema.getTables()) {
      Table oldTable = oldTablesByName.get(newTable.getName());
      List<InitialData> oldData = oldTable == null ? List.of() : oldTable.getInitialData();
      for (InitialData data : newTable.getInitialData()) {
        if (!containsInitialData(oldData, data)) {
          changeSet.addChange(new AddInitialDataChange(newTable.getName(), data));
        }
      }
    }
  }

  private void dropInitialData(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, Table> newTablesByName = indexTablesByName(newSchema);
    for (Table oldTable : oldSchema.getTables()) {
      Table newTable = newTablesByName.get(oldTable.getName());
      if (newTable == null) {
        continue;
      }
      for (InitialData data : oldTable.getInitialData()) {
        if (!containsInitialData(newTable.getInitialData(), data)) {
          changeSet.addChange(new DropInitialDataChange(oldTable.getName(), data));
        }
      }
    }
  }

  private boolean containsInitialData(List<InitialData> entries, InitialData target) {
    return entries.stream()
        .anyMatch(
            d ->
                d.getDatabaseType() == target.getDatabaseType()
                    && trimmed(d.getSql()).equals(trimmed(target.getSql())));
  }

  private String trimmed(String s) {
    return s == null ? "" : s.trim();
  }

  private Map<String, Table> indexTablesByName(Schema schema) {
    Map<String, Table> map = new HashMap<>();
    for (Table table : schema.getTables()) {
      map.put(table.getName(), table);
    }
    return map;
  }

  private Map<String, Column> indexColumnsByName(Table table) {
    Map<String, Column> map = new HashMap<>();
    for (Column column : table.getColumns()) {
      map.put(column.getName(), column);
    }
    return map;
  }

  private Map<String, View> indexViewsByName(Schema schema) {
    Map<String, View> map = new HashMap<>();
    for (View view : schema.getViews()) {
      map.put(view.getName(), view);
    }
    return map;
  }

  private boolean keyExistsIn(Key key, List<Key> keys) {
    for (Key candidate : keys) {
      if (keysEqual(candidate, key)) {
        return true;
      }
    }
    return false;
  }

  private boolean keysEqual(Key a, Key b) {
    if (a.getType() != b.getType()
        || a.isUnique() != b.isUnique()
        || a.isCluster() != b.isCluster()
        || !java.util.Objects.equals(a.getInclude(), b.getInclude())
        || !java.util.Objects.equals(a.getFilter(), b.getFilter())
        || a.getColumns().size() != b.getColumns().size()) {
      return false;
    }

    for (int i = 0; i < a.getColumns().size(); i++) {
      if (!a.getColumns().get(i).getName().equalsIgnoreCase(b.getColumns().get(i).getName())) {
        return false;
      }
    }

    return true;
  }

  private boolean containsConstraint(List<Constraint> constraints, Constraint target) {
    for (Constraint constraint : constraints) {
      if (constraint.getName().equals(target.getName())
          && java.util.Objects.equals(constraint.getDatabaseType(), target.getDatabaseType())
          && java.util.Objects.equals(constraint.getSql(), target.getSql())) {
        return true;
      }
    }
    return false;
  }

  private boolean viewsEqual(View a, View b) {
    return java.util.Objects.equals(a.getSql(), b.getSql())
        && java.util.Objects.equals(a.getDatabaseType(), b.getDatabaseType());
  }

  private boolean containsRelation(List<Relation> relations, Relation target) {
    for (Relation relation : relations) {
      if (relation.getFromTableName().equals(target.getFromTableName())
          && relation.getToTableName().equals(target.getToTableName())
          && relation.getType() == target.getType()
          && columnPairsEqual(relation.getColumnPairs(), target.getColumnPairs())) {
        return true;
      }
    }
    return false;
  }

  private boolean columnPairsEqual(List<ColumnPair> a, List<ColumnPair> b) {
    if (a.size() != b.size()) {
      return false;
    }
    for (int i = 0; i < a.size(); i++) {
      ColumnPair pa = a.get(i);
      ColumnPair pb = b.get(i);
      if (!pa.getFromColumnName().equals(pb.getFromColumnName())
          || !pa.getToColumnName().equals(pb.getToColumnName())) {
        return false;
      }
    }
    return true;
  }

  private boolean columnsEqual(Column col1, Column col2) {
    return col1.getType() == col2.getType()
        && col1.getLength() == col2.getLength()
        && col1.getScale() == col2.getScale()
        && col1.isRequired() == col2.isRequired()
        && java.util.Objects.equals(col1.getDefaultConstraint(), col2.getDefaultConstraint())
        && java.util.Objects.equals(col1.getCheckConstraint(), col2.getCheckConstraint())
        && java.util.Objects.equals(col1.getEnumType(), col2.getEnumType())
        && col1.getElementType() == col2.getElementType()
        && java.util.Objects.equals(col1.getGenerated(), col2.getGenerated())
        && java.util.Objects.equals(col1.getMinValue(), col2.getMinValue())
        && java.util.Objects.equals(col1.getMaxValue(), col2.getMaxValue());
  }

  private Map<String, Function> indexFunctionsByKey(Schema schema) {
    Map<String, Function> map = new HashMap<>();
    for (Function f : schema.getFunctions()) {
      map.put(functionKey(f), f);
    }
    return map;
  }

  private Map<String, Procedure> indexProceduresByKey(Schema schema) {
    Map<String, Procedure> map = new HashMap<>();
    for (Procedure p : schema.getProcedures()) {
      map.put(procedureKey(p), p);
    }
    return map;
  }

  private String functionKey(Function f) {
    return f.getName() + ":" + f.getDatabaseType();
  }

  private String procedureKey(Procedure p) {
    return p.getName() + ":" + p.getDatabaseType();
  }
}
