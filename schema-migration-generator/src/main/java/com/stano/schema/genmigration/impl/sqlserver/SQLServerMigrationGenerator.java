package com.stano.schema.genmigration.impl.sqlserver;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.diff.change.ModifyEnumTypeChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGenerator;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.gensql.impl.common.ColumnGenerator;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.sqlserver.SQLServerColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Naming;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SQLServerMigrationGenerator extends MigrationGenerator {
  private final ColumnTypeMapper mapper;

  public SQLServerMigrationGenerator(MigrationGeneratorOptions options) {
    super(options);
    this.mapper = new SQLServerColumnTypeMapper(BooleanMode.NATIVE, options.getSchema());
  }

  @Override
  protected void generateAddTable(AddTableChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "IF OBJECT_ID('"
            + change.getTableName()
            + "', 'U') IS NULL CREATE TABLE "
            + change.getTableName()
            + " ()");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropTable(DropTableChange change) {
    PrintWriter w = options.getWriter();
    w.println("DROP TABLE IF EXISTS " + change.getTableName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateRenameTable(RenameTableChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "IF OBJECT_ID('"
            + change.getOldName()
            + "', 'U') IS NOT NULL AND OBJECT_ID('"
            + change.getNewName()
            + "', 'U') IS NULL EXEC sp_rename '"
            + change.getOldName()
            + "', '"
            + change.getNewName()
            + "'");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateRenameColumn(RenameColumnChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('"
            + change.getTableName()
            + "') AND name = '"
            + change.getOldName()
            + "') AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('"
            + change.getTableName()
            + "') AND name = '"
            + change.getNewName()
            + "') EXEC sp_rename '"
            + change.getTableName()
            + "."
            + change.getOldName()
            + "', '"
            + change.getNewName()
            + "', 'COLUMN'");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddColumn(AddColumnChange change) {
    PrintWriter w = options.getWriter();
    Column col = change.getColumn();
    String table = change.getTableName();
    String missing = columnMissingCondition(table, col.getName());

    if (col.getGenerated() != null) {
      // Computed columns declare no type, take no default, and are always calculated for existing
      // rows - nothing to backfill.
      w.println(
          "IF "
              + missing
              + " ALTER TABLE "
              + table
              + " ADD "
              + col.getName()
              + " AS ("
              + col.getGenerated()
              + ") PERSISTED"
              + (col.isRequired() ? " NOT NULL" : ""));
      w.print(options.getStatementSeparator());
      w.println();
    } else if (col.isRequired() && col.getDefaultConstraint() == null && !isIdentity(col)) {
      // An identity column numbers existing rows itself, so there is nothing to backfill.
      writeNotNullColumnWithBackfillGuard(table, col);
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("IF ")
          .append(missing)
          .append(" ALTER TABLE ")
          .append(table)
          .append(" ADD ")
          .append(col.getName())
          .append(" ")
          .append(mapper.toSqlType(col))
          .append(col.isRequired() || isIdentity(col) ? " NOT NULL" : " NULL");
      if (col.getDefaultConstraint() != null) {
        sb.append(" CONSTRAINT ")
            .append(ColumnGenerator.defaultConstraintName(table, col.getName()))
            .append(" DEFAULT ")
            .append(col.getDefaultConstraint());
      }
      w.println(sb.toString());
      w.print(options.getStatementSeparator());
      w.println();
    }

    writeAddCheckConstraint(table, col);
  }

  private static boolean isIdentity(Column column) {
    return column.getType() == ColumnType.SEQUENCE || column.getType() == ColumnType.LONGSEQUENCE;
  }

  private static String columnMissingCondition(String table, String column) {
    return "NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('"
        + table
        + "') AND name = '"
        + column
        + "')";
  }

  /**
   * A required column with no default has no value to backfill existing rows with, so adding it
   * {@code NOT NULL} would fail the moment the table has any rows. Adds the column nullable, halts
   * unconditionally so the developer must either backfill or confirm the table is empty (never a
   * silent pass-through), then tightens it to {@code NOT NULL} - reachable only once the halt has
   * been deleted.
   */
  private void writeNotNullColumnWithBackfillGuard(String table, Column col) {
    PrintWriter w = options.getWriter();
    String type = mapper.toSqlType(col);
    w.println(
        "IF "
            + columnMissingCondition(table, col.getName())
            + " ALTER TABLE "
            + table
            + " ADD "
            + col.getName()
            + " "
            + type
            + " NULL");
    w.print(options.getStatementSeparator());
    w.println();

    writeBackfillWarning(table, col.getName());
    w.println(
        "THROW 50000, 'Migration halted: "
            + table
            + " has no value for "
            + col.getName()
            + " - see comment above.', 1");
    w.print(options.getStatementSeparator());
    w.println();

    w.println("ALTER TABLE " + table + " ALTER COLUMN " + col.getName() + " " + type + " NOT NULL");
    w.print(options.getStatementSeparator());
    w.println();
  }

  private static String columnExistsGuard(String table, String column) {
    return "IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('"
        + table
        + "') AND name = '"
        + column
        + "') ";
  }

  private void writeAddCheckConstraint(String table, Column col) {
    String checkSql = getCheckConstraintSql(col);
    if (checkSql == null) {
      return;
    }
    PrintWriter w = options.getWriter();
    String constraintName = getCheckConstraintName(table, col.getName());
    w.println(
        "IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = '"
            + constraintName
            + "') ALTER TABLE "
            + table
            + " ADD CONSTRAINT "
            + constraintName
            + " "
            + checkSql);
    w.print(options.getStatementSeparator());
    w.println();
  }

  private void writeDropDefaultConstraint(String table, String column) {
    PrintWriter w = options.getWriter();
    String name = ColumnGenerator.defaultConstraintName(table, column);
    w.println(
        "IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = '"
            + name
            + "') ALTER TABLE "
            + table
            + " DROP CONSTRAINT "
            + name);
    w.print(options.getStatementSeparator());
    w.println();
  }

  private void writeDropCheckConstraint(String table, String column) {
    PrintWriter w = options.getWriter();
    String name = getCheckConstraintName(table, column);
    w.println(
        "IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = '"
            + name
            + "') ALTER TABLE "
            + table
            + " DROP CONSTRAINT "
            + name);
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropColumn(DropColumnChange change) {
    PrintWriter w = options.getWriter();
    // DROP COLUMN fails while a DEFAULT or CHECK constraint still references the column. Both are
    // dropped by their computed (create-path) names first, guarded so it is a no-op when absent.
    writeDropDefaultConstraint(change.getTableName(), change.getColumnName());
    writeDropCheckConstraint(change.getTableName(), change.getColumnName());
    w.println(
        "IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('"
            + change.getTableName()
            + "') AND name = '"
            + change.getColumnName()
            + "') ALTER TABLE "
            + change.getTableName()
            + " DROP COLUMN "
            + change.getColumnName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateModifyColumn(ModifyColumnChange change) {
    PrintWriter w = options.getWriter();
    Column oldCol = change.getOldColumn();
    Column newCol = change.getNewColumn();
    String table = change.getTableName();

    if (generatedChanged(oldCol, newCol)) {
      writeGeneratedChangeWarning(table, newCol.getName());
      w.println(
          "THROW 50000, 'Migration halted: generated column "
              + table
              + "."
              + newCol.getName()
              + " changed - see comment above.', 1");
      w.print(options.getStatementSeparator());
      w.println();
    }

    // SQL Server refuses ALTER COLUMN outright while the column participates in a PRIMARY
    // KEY/UNIQUE constraint, a plain index, or a foreign key (either as the FK's own column, or as
    // the column another table's FK references). Find and drop them first, guarded, in dependency
    // order (a relation can depend on a unique/PK index, so it comes off first and goes back last).
    List<AddKeyChange> blockingKeys = dependentKeys(table, oldCol.getName());
    List<AddRelationChange> blockingRelations = dependentRelations(table, oldCol.getName());

    for (AddRelationChange relation : blockingRelations) {
      generateDropRelation(new DropRelationChange(relation.getRelation(), relation.getOrdinal()));
    }
    for (AddKeyChange key : blockingKeys) {
      generateDropKey(new DropKeyChange(key.getTableName(), key.getKey(), key.getOrdinal()));
    }

    // ALTER COLUMN also refuses while a DEFAULT or CHECK constraint from the old definition still
    // references the column, so drop both first (keyed off the old name; this is not a rename).
    writeDropDefaultConstraint(table, oldCol.getName());
    writeDropCheckConstraint(table, oldCol.getName());

    // IDENTITY can only be set when a column is created - ALTER COLUMN rejects an identity clause
    // outright. Strip it to the bare integer type; this can still change nullability, but cannot
    // retarget an identity column's underlying type - that needs a manual column rebuild, so flag
    // it instead of emitting DDL that would either fail or silently do the wrong thing.
    boolean identity =
        newCol.getType() == ColumnType.SEQUENCE || newCol.getType() == ColumnType.LONGSEQUENCE;
    if (identity) {
      w.println(
          "-- NOTE: '"
              + newCol.getName()
              + "' is an identity column - IDENTITY cannot be changed via ALTER COLUMN. If its"
              + " underlying type needs to change too, this requires a manual column rebuild.");
    }
    String type =
        identity
            ? (newCol.getType() == ColumnType.SEQUENCE ? "integer" : "bigint")
            : mapper.toSqlType(newCol);
    w.println(
        "ALTER TABLE "
            + table
            + " ALTER COLUMN "
            + newCol.getName()
            + " "
            + type
            + (newCol.isRequired() ? " NOT NULL" : " NULL"));
    w.print(options.getStatementSeparator());
    w.println();

    if (newCol.getDefaultConstraint() != null) {
      String name = ColumnGenerator.defaultConstraintName(table, newCol.getName());
      w.println(
          "IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = '"
              + name
              + "') ALTER TABLE "
              + table
              + " ADD CONSTRAINT "
              + name
              + " DEFAULT "
              + newCol.getDefaultConstraint()
              + " FOR "
              + newCol.getName());
      w.print(options.getStatementSeparator());
      w.println();
    }
    writeAddCheckConstraint(table, newCol);

    for (AddKeyChange key : blockingKeys) {
      generateAddKey(key);
    }
    for (AddRelationChange relation : blockingRelations) {
      generateAddRelation(relation);
    }
  }

  /**
   * Every key and index of {@code tableName} that contains {@code columnName}, paired with the
   * ordinal the create path numbers it with (unique keys among unique keys, indexes among indexes),
   * so the generated name matches the existing object.
   */
  private List<AddKeyChange> dependentKeys(String tableName, String columnName) {
    List<AddKeyChange> found = new ArrayList<>();
    Schema schema = options.getSchema();
    if (schema == null) {
      return found;
    }
    Optional<Table> table = schema.getOptionalTable(tableName);
    if (table.isEmpty()) {
      return found;
    }

    int uniqueOrdinal = 0;
    for (Key key : table.get().getKeys()) {
      if (key.getType() == KeyType.UNIQUE) {
        uniqueOrdinal++;
      }
      if (key.containsColumn(columnName)) {
        found.add(new AddKeyChange(tableName, key, uniqueOrdinal));
      }
    }

    int indexOrdinal = 0;
    for (Key key : table.get().getIndexes()) {
      if (key.getType() == KeyType.INDEX) {
        indexOrdinal++;
      }
      if (key.containsColumn(columnName)) {
        found.add(new AddKeyChange(tableName, key, indexOrdinal));
      }
    }
    return found;
  }

  /**
   * Every foreign key in the schema that touches {@code tableName.columnName} - either as the FK's
   * own column or as the column another table's FK references - paired with the relation's 1-based
   * ordinal among its owning table's relations, so the generated name matches the existing
   * constraint. SQL Server blocks ALTER COLUMN for both roles.
   */
  private List<AddRelationChange> dependentRelations(String tableName, String columnName) {
    List<AddRelationChange> found = new ArrayList<>();
    Schema schema = options.getSchema();
    if (schema == null) {
      return found;
    }

    for (Table owner : schema.getTables()) {
      for (int i = 0; i < owner.getRelations().size(); i++) {
        Relation relation = owner.getRelations().get(i);
        if (!relation.createsForeignKeyConstraint()) {
          continue;
        }
        boolean fromMatches =
            owner.getName().equalsIgnoreCase(tableName)
                && relation.getColumnPairs().stream()
                    .anyMatch(p -> p.getFromColumnName().equalsIgnoreCase(columnName));
        boolean toMatches =
            relation.getToTableName().equalsIgnoreCase(tableName)
                && relation.getColumnPairs().stream()
                    .anyMatch(p -> p.getToColumnName().equalsIgnoreCase(columnName));
        if (fromMatches || toMatches) {
          found.add(new AddRelationChange(relation, i + 1));
        }
      }
    }
    return found;
  }

  @Override
  protected void generateAddKey(AddKeyChange change) {
    PrintWriter w = options.getWriter();
    switch (change.getKey().getType()) {
      case PRIMARY:
        {
          String pkName = Naming.primaryKeyName(DatabaseType.SQL_SERVER, change.getTableName());
          w.println(
              "IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = '"
                  + pkName
                  + "' AND parent_object_id = OBJECT_ID('"
                  + change.getTableName()
                  + "')) ALTER TABLE "
                  + change.getTableName()
                  + " ADD CONSTRAINT "
                  + pkName
                  + " PRIMARY KEY ("
                  + change.getKey().getColumnsAsString()
                  + ")");
        }
        break;
      case UNIQUE:
        {
          String constraintName =
              Naming.uniqueKeyName(
                  DatabaseType.SQL_SERVER, change.getTableName(), change.getOrdinal());
          w.println(
              "IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = '"
                  + constraintName
                  + "' AND parent_object_id = OBJECT_ID('"
                  + change.getTableName()
                  + "')) ALTER TABLE "
                  + change.getTableName()
                  + " ADD CONSTRAINT "
                  + constraintName
                  + " UNIQUE ("
                  + change.getKey().getColumnsAsString()
                  + ")");
        }
        break;
      case INDEX:
        {
          String indexName =
              Naming.indexName(DatabaseType.SQL_SERVER, change.getTableName(), change.getOrdinal());
          w.println(
              "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = '"
                  + indexName
                  + "') CREATE "
                  + (change.getKey().isUnique() ? "UNIQUE " : "")
                  + "INDEX "
                  + indexName
                  + " ON "
                  + change.getTableName()
                  + " ("
                  + change.getKey().getColumnsAsString()
                  + ")"
                  + (change.getKey().getFilter() != null
                      ? " WHERE " + change.getKey().getFilter()
                      : ""));
        }
        break;
    }
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropKey(DropKeyChange change) {
    PrintWriter w = options.getWriter();
    switch (change.getKey().getType()) {
      case PRIMARY:
        {
          String pkName = Naming.primaryKeyName(DatabaseType.SQL_SERVER, change.getTableName());
          w.println(
              "IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = '"
                  + pkName
                  + "' AND parent_object_id = OBJECT_ID('"
                  + change.getTableName()
                  + "')) ALTER TABLE "
                  + change.getTableName()
                  + " DROP CONSTRAINT "
                  + pkName);
        }
        break;
      case UNIQUE:
        {
          String constraintName =
              Naming.uniqueKeyName(
                  DatabaseType.SQL_SERVER, change.getTableName(), change.getOrdinal());
          w.println(
              "IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = '"
                  + constraintName
                  + "' AND parent_object_id = OBJECT_ID('"
                  + change.getTableName()
                  + "')) ALTER TABLE "
                  + change.getTableName()
                  + " DROP CONSTRAINT "
                  + constraintName);
        }
        break;
      case INDEX:
        {
          String indexName =
              Naming.indexName(DatabaseType.SQL_SERVER, change.getTableName(), change.getOrdinal());
          w.println(
              "IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = '"
                  + indexName
                  + "') DROP INDEX "
                  + indexName
                  + " ON "
                  + change.getTableName());
        }
        break;
    }
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddConstraint(AddConstraintChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = '"
            + change.getConstraint().getName()
            + "') ALTER TABLE "
            + change.getTableName()
            + " ADD CONSTRAINT "
            + change.getConstraint().getName()
            + " "
            + change.getConstraint().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropConstraint(DropConstraintChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "IF EXISTS (SELECT 1 FROM sys.objects WHERE name = '"
            + change.getConstraintName()
            + "' AND parent_object_id = OBJECT_ID('"
            + change.getTableName()
            + "')) ALTER TABLE "
            + change.getTableName()
            + " DROP CONSTRAINT "
            + change.getConstraintName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddRelation(AddRelationChange change) {
    PrintWriter w = options.getWriter();
    String fkName =
        Naming.foreignKeyName(
            DatabaseType.SQL_SERVER, change.getRelation().getFromTableName(), change.getOrdinal());
    String onDelete = onDeleteClause(change.getRelation());
    w.println(
        "IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = '"
            + fkName
            + "') ALTER TABLE "
            + change.getRelation().getFromTableName()
            + " ADD CONSTRAINT "
            + fkName
            + " FOREIGN KEY ("
            + change.getRelation().getColumnPairs().stream()
                .map(ColumnPair::getFromColumnName)
                .collect(Collectors.joining(", "))
            + ") REFERENCES "
            + change.getRelation().getToTableName()
            + "("
            + change.getRelation().getColumnPairs().stream()
                .map(ColumnPair::getToColumnName)
                .collect(Collectors.joining(", "))
            + ")"
            + onDelete);
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropRelation(DropRelationChange change) {
    PrintWriter w = options.getWriter();
    String fkName =
        Naming.foreignKeyName(
            DatabaseType.SQL_SERVER, change.getRelation().getFromTableName(), change.getOrdinal());
    w.println(
        "IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = '"
            + fkName
            + "') ALTER TABLE "
            + change.getRelation().getFromTableName()
            + " DROP CONSTRAINT "
            + fkName);
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddFunction(AddFunctionChange change) {
    if (change.getFunction().getDatabaseType() != DatabaseType.SQL_SERVER) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println(change.getFunction().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropFunction(DropFunctionChange change) {
    if (change.getDatabaseType() != DatabaseType.SQL_SERVER) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println("DROP FUNCTION IF EXISTS dbo." + change.getFunctionName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddProcedure(AddProcedureChange change) {
    if (change.getProcedure().getDatabaseType() != DatabaseType.SQL_SERVER) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println(change.getProcedure().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropProcedure(DropProcedureChange change) {
    if (change.getDatabaseType() != DatabaseType.SQL_SERVER) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println("DROP PROCEDURE IF EXISTS dbo." + change.getProcedureName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddView(AddViewChange change) {
    PrintWriter w = options.getWriter();
    w.println("CREATE VIEW " + change.getView().getName() + " AS " + change.getView().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropView(DropViewChange change) {
    PrintWriter w = options.getWriter();
    w.println("DROP VIEW IF EXISTS " + change.getViewName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  /**
   * SQL Server has no native enum type: an enum is a {@code CHECK} constraint on each column that
   * uses it. A removed value halts the migration unconditionally (rows may still hold it, and
   * failing only when such a row happens to exist would hide the decision); then each such column's
   * check constraint is dropped and recreated from the new value list.
   */
  @Override
  protected void generateModifyEnumType(ModifyEnumTypeChange change) {
    PrintWriter w = options.getWriter();
    String name = change.getNewEnumType().getName();

    for (EnumValue value : removedEnumValues(change)) {
      String code = value.getCode();
      w.println("-- WARNING: enum value '" + code + "' was removed from '" + name + "'.");
      w.println(
          "--   1. If rows still use '"
              + code
              + "', add UPDATE statements above this line to migrate them to a valid value, then"
              + " delete the block below, OR");
      w.println("--   2. If no rows use '" + code + "', delete the block below to confirm that.");
      w.print(
          "THROW 50000, 'Migration halted: enum value ''"
              + escapeSqlLiteral(code)
              + "'' was removed from "
              + escapeSqlLiteral(name)
              + " - see comment above.', 1");
      w.print(options.getStatementSeparator());
      w.println();
    }

    if (options.getSchema() == null) {
      return;
    }
    for (Table table : options.getSchema().getTables()) {
      for (Column column : table.getColumns()) {
        if (name.equalsIgnoreCase(column.getEnumType())) {
          // The enum change runs before this migration's new tables and columns exist; those get
          // the new check from their own add, so only cycle columns that are already there.
          String guard = columnExistsGuard(table.getName(), column.getName());
          w.print(guard);
          writeDropCheckConstraint(table.getName(), column.getName());
          w.print(guard);
          writeAddCheckConstraint(table.getName(), column);
        }
      }
    }
  }
}
