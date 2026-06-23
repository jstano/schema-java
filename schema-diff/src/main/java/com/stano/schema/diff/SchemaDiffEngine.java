package com.stano.schema.diff;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.model.Column;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.Key;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaDiffEngine {

  public ChangeSet diff(Schema oldSchema, Schema newSchema) {
    ChangeSet changeSet = new ChangeSet();

    // Drop phase: views → relations → keys → constraints → columns → tables
    dropViews(changeSet, oldSchema, newSchema);
    dropRelations(changeSet, oldSchema, newSchema);
    dropKeys(changeSet, oldSchema, newSchema);
    dropConstraints(changeSet, oldSchema, newSchema);
    dropColumns(changeSet, oldSchema, newSchema);
    dropTables(changeSet, oldSchema, newSchema);

    // Add phase: tables → columns → modify columns → keys → constraints → relations → views
    addTables(changeSet, oldSchema, newSchema);
    addColumns(changeSet, oldSchema, newSchema);
    modifyColumns(changeSet, oldSchema, newSchema);
    addKeys(changeSet, oldSchema, newSchema);
    addConstraints(changeSet, oldSchema, newSchema);
    addRelations(changeSet, oldSchema, newSchema);
    addViews(changeSet, oldSchema, newSchema);

    return changeSet;
  }

  private void dropViews(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, View> newViewsByName = indexViewsByName(newSchema);
    for (View oldView : oldSchema.getViews()) {
      View newView = newViewsByName.get(oldView.getName());
      if (newView == null) {
        changeSet.addChange(new DropViewChange(oldView.getName()));
      } else if (!java.util.Objects.equals(oldView.getSql(), newView.getSql())) {
        // View SQL changed: drop the old one so addViews() re-creates it with the new SQL.
        changeSet.addChange(new DropViewChange(oldView.getName()));
      }
    }
  }

  private void dropRelations(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    List<Relation> newRelations = getAllRelations(newSchema);
    for (Relation oldRelation : getAllRelations(oldSchema)) {
      if (!containsRelation(newRelations, oldRelation)) {
        changeSet.addChange(new DropRelationChange(oldRelation));
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
      for (Key oldKey : oldTable.getKeys()) {
        if (!containsKey(newTable.getKeys(), oldKey)) {
          changeSet.addChange(new DropKeyChange(oldTable.getName(), oldKey));
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
              new DropConstraintChange(oldTable.getName(), oldConstraint.getName()));
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
      for (Column oldColumn : oldTable.getColumns()) {
        if (!newColumnsByName.containsKey(oldColumn.getName())) {
          changeSet.addChange(new DropColumnChange(oldTable.getName(), oldColumn.getName()));
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
        for (Key newKey : newTable.getKeys()) {
          changeSet.addChange(new AddKeyChange(newTable.getName(), newKey));
        }
        continue;
      }
      for (Key newKey : newTable.getKeys()) {
        if (!containsKey(oldTable.getKeys(), newKey)) {
          changeSet.addChange(new AddKeyChange(newTable.getName(), newKey));
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
    List<Relation> oldRelations = getAllRelations(oldSchema);
    for (Relation newRelation : getAllRelations(newSchema)) {
      if (!containsRelation(oldRelations, newRelation)) {
        changeSet.addChange(new AddRelationChange(newRelation));
      }
    }
  }

  private void addViews(ChangeSet changeSet, Schema oldSchema, Schema newSchema) {
    Map<String, View> oldViewsByName = indexViewsByName(oldSchema);
    for (View newView : newSchema.getViews()) {
      View oldView = oldViewsByName.get(newView.getName());
      if (oldView == null) {
        changeSet.addChange(new AddViewChange(newView));
      } else if (!java.util.Objects.equals(oldView.getSql(), newView.getSql())) {
        // dropViews already emitted a DropViewChange; emit the AddViewChange with new SQL.
        changeSet.addChange(new AddViewChange(newView));
      }
    }
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

  private boolean containsKey(List<Key> keys, Key target) {
    for (Key key : keys) {
      if (key.getType() == target.getType()
          && key.getColumnsAsString().equals(target.getColumnsAsString())) {
        return true;
      }
    }
    return false;
  }

  private boolean containsConstraint(List<Constraint> constraints, Constraint target) {
    for (Constraint constraint : constraints) {
      if (constraint.getName().equals(target.getName())
          && java.util.Objects.equals(constraint.getSql(), target.getSql())) {
        return true;
      }
    }
    return false;
  }

  private boolean containsRelation(List<Relation> relations, Relation target) {
    for (Relation relation : relations) {
      if (relation.getFromTableName().equals(target.getFromTableName())
          && relation.getFromColumnName().equals(target.getFromColumnName())
          && relation.getToTableName().equals(target.getToTableName())
          && relation.getToColumnName().equals(target.getToColumnName())) {
        return true;
      }
    }
    return false;
  }

  private List<Relation> getAllRelations(Schema schema) {
    List<Relation> allRelations = new ArrayList<>();
    for (Table table : schema.getTables()) {
      allRelations.addAll(table.getRelations());
    }
    return allRelations;
  }

  private boolean columnsEqual(Column col1, Column col2) {
    return col1.getType() == col2.getType()
        && col1.getLength() == col2.getLength()
        && col1.getScale() == col2.getScale()
        && col1.isRequired() == col2.isRequired()
        && java.util.Objects.equals(col1.getDefaultConstraint(), col2.getDefaultConstraint());
  }
}
