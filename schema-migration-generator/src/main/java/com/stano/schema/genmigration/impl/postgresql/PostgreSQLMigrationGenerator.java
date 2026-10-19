package com.stano.schema.genmigration.impl.postgresql;

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
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGenerator;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.postgresql.PostgreSQLColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Naming;
import com.stano.schema.model.Schema;
import com.stano.schema.model.View;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PostgreSQLMigrationGenerator extends MigrationGenerator {
  private final ColumnTypeMapper mapper;

  public PostgreSQLMigrationGenerator(MigrationGeneratorOptions options) {
    super(options);
    this.mapper = new PostgreSQLColumnTypeMapper(BooleanMode.NATIVE, options.getSchema());
  }

  @Override
  protected void generateAddTable(AddTableChange change) {
    PrintWriter w = options.getWriter();
    w.println("CREATE TABLE IF NOT EXISTS " + change.getTableName() + " ()");
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
    w.println("ALTER TABLE IF EXISTS " + change.getOldName() + " RENAME TO " + change.getNewName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateRenameColumn(RenameColumnChange change) {
    PrintWriter w = options.getWriter();
    w.println("DO $$");
    w.println("BEGIN");
    w.println(
        "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = '"
            + change.getTableName()
            + "' AND column_name = '"
            + change.getOldName()
            + "') THEN");
    w.println(
        "    ALTER TABLE "
            + change.getTableName()
            + " RENAME COLUMN "
            + change.getOldName()
            + " TO "
            + change.getNewName()
            + ";");
    w.println("  END IF;");
    w.println("END $$");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddColumn(AddColumnChange change) {
    PrintWriter w = options.getWriter();
    Column col = change.getColumn();
    String table = change.getTableName();

    if (col.getGenerated() != null) {
      // Generated columns take no default, and the value is computed for existing rows, so there
      // is nothing to backfill.
      w.println(
          "ALTER TABLE "
              + table
              + " ADD COLUMN IF NOT EXISTS "
              + col.getName()
              + " "
              + mapper.toSqlType(col)
              + " GENERATED ALWAYS AS ("
              + col.getGenerated()
              + ") STORED"
              + (col.isRequired() ? " NOT NULL" : ""));
      w.print(options.getStatementSeparator());
      w.println();
    } else if (col.isRequired() && col.getDefaultConstraint() == null) {
      writeNotNullColumnWithBackfillGuard(table, col);
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("ALTER TABLE ")
          .append(table)
          .append(" ADD COLUMN IF NOT EXISTS ")
          .append(col.getName())
          .append(" ")
          .append(mapper.toSqlType(col));
      if (col.getDefaultConstraint() != null) {
        sb.append(" DEFAULT ").append(col.getDefaultConstraint());
      }
      w.println(sb.toString());
      w.print(options.getStatementSeparator());
      w.println();

      if (col.isRequired()) {
        w.println("ALTER TABLE " + table + " ALTER COLUMN " + col.getName() + " SET NOT NULL");
        w.print(options.getStatementSeparator());
        w.println();
      }
    }

    String checkSql = getCheckConstraintSql(col);
    if (checkSql != null) {
      String constraintName = getCheckConstraintName(table, col.getName());
      writeGuardedAddConstraint(
          w,
          table,
          constraintName,
          "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName + " " + checkSql + ";");
    }
  }

  /**
   * A required column with no default has no value to backfill existing rows with, so {@code ADD
   * COLUMN ... NOT NULL} would fail the moment the table has any rows. Adds the column nullable,
   * halts unconditionally so the developer must either backfill or confirm the table is empty
   * (never a silent pass-through), then tightens it to {@code NOT NULL} - reachable only once the
   * halt has been deleted.
   */
  private void writeNotNullColumnWithBackfillGuard(String table, Column col) {
    PrintWriter w = options.getWriter();
    w.println(
        "ALTER TABLE "
            + table
            + " ADD COLUMN IF NOT EXISTS "
            + col.getName()
            + " "
            + mapper.toSqlType(col));
    w.print(options.getStatementSeparator());
    w.println();

    writeBackfillWarning(table, col.getName());
    w.println("DO $$");
    w.println("BEGIN");
    w.println(
        "  RAISE EXCEPTION 'Migration halted: "
            + table
            + " has no value for "
            + col.getName()
            + " - see comment above.';");
    w.println("END $$");
    w.print(options.getStatementSeparator());
    w.println();

    w.println("ALTER TABLE " + table + " ALTER COLUMN " + col.getName() + " SET NOT NULL");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropColumn(DropColumnChange change) {
    PrintWriter w = options.getWriter();
    List<View> dependentViews = dependentViews(change.getTableName(), change.getColumnName());
    writeDropViews(dependentViews);
    w.println(
        "ALTER TABLE "
            + change.getTableName()
            + " DROP COLUMN IF EXISTS "
            + change.getColumnName());
    w.print(options.getStatementSeparator());
    w.println();
    writeRecreateViews(dependentViews);
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
          "DO $$ BEGIN RAISE EXCEPTION 'Migration halted: generated column "
              + table
              + "."
              + newCol.getName()
              + " changed - see comment above.'; END $$");
      w.print(options.getStatementSeparator());
      w.println();
    }

    String newType = alterColumnType(newCol);
    boolean typeChanged = !newType.equals(alterColumnType(oldCol));

    // Postgres refuses ALTER COLUMN TYPE on a column a view reads ("cannot alter type of a column
    // used by a view or rule"), so any dependent view is dropped first and recreated after the
    // whole change.
    List<View> dependentViews = typeChanged ? dependentViews(table, oldCol.getName()) : List.of();
    writeDropViews(dependentViews);

    if (typeChanged) {
      if (isSequence(newCol)) {
        w.println(
            "-- NOTE: '"
                + newCol.getName()
                + "' is a SEQUENCE/LONGSEQUENCE column - serial/bigserial is create-time-only sugar"
                + " for "
                + newType
                + " + an owned sequence + a default, so ALTER COLUMN TYPE below only retargets the"
                + " underlying type. If this column needs an owned sequence and default too, that"
                + " requires manual DDL (CREATE SEQUENCE ... OWNED BY ..., then SET DEFAULT"
                + " nextval(...)).");
      }
      // USING makes the cast explicit instead of relying on Postgres's implicit assignment cast,
      // which doesn't exist for many pairs (e.g. varchar -> int: "column cannot be cast
      // automatically"). It also covers every case the implicit cast already handled.
      w.println(
          "ALTER TABLE "
              + table
              + " ALTER COLUMN "
              + newCol.getName()
              + " TYPE "
              + newType
              + " USING "
              + newCol.getName()
              + "::"
              + newType);
      w.print(options.getStatementSeparator());
      w.println();
    }

    if (oldCol.isRequired() != newCol.isRequired()) {
      w.println(
          "ALTER TABLE "
              + table
              + " ALTER COLUMN "
              + newCol.getName()
              + (newCol.isRequired() ? " SET NOT NULL" : " DROP NOT NULL"));
      w.print(options.getStatementSeparator());
      w.println();
    }

    if (!Objects.equals(oldCol.getDefaultConstraint(), newCol.getDefaultConstraint())) {
      w.println(
          "ALTER TABLE "
              + table
              + " ALTER COLUMN "
              + newCol.getName()
              + (newCol.getDefaultConstraint() != null
                  ? " SET DEFAULT " + newCol.getDefaultConstraint()
                  : " DROP DEFAULT"));
      w.print(options.getStatementSeparator());
      w.println();
    }

    writeRecreateViews(dependentViews);
  }

  private static boolean isSequence(Column column) {
    return column.getType() == ColumnType.SEQUENCE || column.getType() == ColumnType.LONGSEQUENCE;
  }

  /**
   * The mapper emits {@code serial}/{@code bigserial} for sequence columns, which are create-time
   * only sugar (an integer plus an owned sequence and default) and not real type names, so ALTER
   * COLUMN TYPE needs the underlying type instead.
   */
  private String alterColumnType(Column column) {
    return switch (column.getType()) {
      case SEQUENCE -> "integer";
      case LONGSEQUENCE -> "bigint";
      default -> mapper.toSqlType(column);
    };
  }

  /**
   * Views of the new schema that read {@code table.column}, transitively (a view built on a
   * dependent view is dependent too), in schema order. Views carry only raw SQL, so dependence is a
   * word-boundary text match: the view must mention the table and either the column or {@code *}.
   * Over-matching only costs a harmless drop and recreate; under-matching would fail the ALTER.
   */
  private List<View> dependentViews(String tableName, String columnName) {
    Schema schema = options.getSchema();
    if (schema == null) {
      return List.of();
    }

    List<View> all = schema.getViews(DatabaseType.POSTGRESQL);
    String table = tableName.substring(tableName.lastIndexOf('.') + 1).toLowerCase();
    String column = columnName.toLowerCase();

    boolean[] selected = new boolean[all.size()];
    for (int i = 0; i < all.size(); i++) {
      String sql = all.get(i).getSql().toLowerCase();
      selected[i] = containsWord(sql, table) && (containsWord(sql, column) || sql.contains("*"));
    }

    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < all.size(); i++) {
        if (selected[i]) {
          continue;
        }
        String sql = all.get(i).getSql().toLowerCase();
        for (int j = 0; j < all.size(); j++) {
          if (selected[j] && containsWord(sql, all.get(j).getName().toLowerCase())) {
            selected[i] = true;
            changed = true;
            break;
          }
        }
      }
    }

    List<View> result = new ArrayList<>();
    for (int i = 0; i < all.size(); i++) {
      if (selected[i]) {
        result.add(all.get(i));
      }
    }
    return result;
  }

  private static boolean containsWord(String haystack, String needle) {
    int from = 0;
    while (true) {
      int start = haystack.indexOf(needle, from);
      if (start < 0) {
        return false;
      }
      int end = start + needle.length();
      boolean beforeOk = start == 0 || !isIdentifierChar(haystack.charAt(start - 1));
      boolean afterOk = end >= haystack.length() || !isIdentifierChar(haystack.charAt(end));
      if (beforeOk && afterOk) {
        return true;
      }
      from = start + 1;
    }
  }

  private static boolean isIdentifierChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  /** Drops in reverse schema order so a view is dropped before the views it depends on. */
  private void writeDropViews(List<View> views) {
    PrintWriter w = options.getWriter();
    for (View view : views.reversed()) {
      w.println("DROP VIEW IF EXISTS " + view.getName());
      w.print(options.getStatementSeparator());
      w.println();
    }
  }

  private void writeRecreateViews(List<View> views) {
    PrintWriter w = options.getWriter();
    for (View view : views) {
      w.println("CREATE OR REPLACE VIEW " + view.getName() + " AS " + view.getSql());
      w.print(options.getStatementSeparator());
      w.println();
    }
  }

  @Override
  protected void generateAddKey(AddKeyChange change) {
    PrintWriter w = options.getWriter();
    switch (change.getKey().getType()) {
      case PRIMARY:
        {
          String pkName = Naming.primaryKeyName(DatabaseType.POSTGRESQL, change.getTableName());
          writeGuardedAddConstraint(
              w,
              change.getTableName(),
              pkName,
              "ALTER TABLE "
                  + change.getTableName()
                  + " ADD CONSTRAINT "
                  + pkName
                  + " PRIMARY KEY ("
                  + change.getKey().getColumnsAsString()
                  + ");");
        }
        break;
      case UNIQUE:
        {
          String constraintName =
              Naming.uniqueKeyName(
                  DatabaseType.POSTGRESQL, change.getTableName(), change.getOrdinal());
          writeGuardedAddConstraint(
              w,
              change.getTableName(),
              constraintName,
              "ALTER TABLE "
                  + change.getTableName()
                  + " ADD CONSTRAINT "
                  + constraintName
                  + " UNIQUE ("
                  + change.getKey().getColumnsAsString()
                  + ");");
        }
        break;
      case INDEX:
        w.println(
            "CREATE "
                + (change.getKey().isUnique() ? "UNIQUE " : "")
                + "INDEX IF NOT EXISTS "
                + Naming.indexName(
                    DatabaseType.POSTGRESQL, change.getTableName(), change.getOrdinal())
                + " ON "
                + change.getTableName()
                + " ("
                + change.getKey().getColumnsAsString()
                + ")"
                + (change.getKey().getFilter() != null
                    ? " WHERE " + change.getKey().getFilter()
                    : ""));
        w.print(options.getStatementSeparator());
        w.println();
        break;
    }
  }

  @Override
  protected void generateDropKey(DropKeyChange change) {
    PrintWriter w = options.getWriter();
    switch (change.getKey().getType()) {
      case PRIMARY:
        w.println(
            "ALTER TABLE "
                + change.getTableName()
                + " DROP CONSTRAINT IF EXISTS "
                + Naming.primaryKeyName(DatabaseType.POSTGRESQL, change.getTableName()));
        break;
      case UNIQUE:
        w.println(
            "ALTER TABLE "
                + change.getTableName()
                + " DROP CONSTRAINT IF EXISTS "
                + Naming.uniqueKeyName(
                    DatabaseType.POSTGRESQL, change.getTableName(), change.getOrdinal()));
        break;
      case INDEX:
        w.println(
            "DROP INDEX IF EXISTS "
                + Naming.indexName(
                    DatabaseType.POSTGRESQL, change.getTableName(), change.getOrdinal()));
        break;
    }
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddConstraint(AddConstraintChange change) {
    PrintWriter w = options.getWriter();
    writeGuardedAddConstraint(
        w,
        change.getTableName(),
        change.getConstraint().getName(),
        "ALTER TABLE "
            + change.getTableName()
            + " ADD CONSTRAINT "
            + change.getConstraint().getName()
            + " "
            + change.getConstraint().getSql()
            + ";");
  }

  @Override
  protected void generateDropConstraint(DropConstraintChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "ALTER TABLE "
            + change.getTableName()
            + " DROP CONSTRAINT IF EXISTS "
            + change.getConstraintName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddRelation(AddRelationChange change) {
    PrintWriter w = options.getWriter();
    String fkName =
        Naming.foreignKeyName(
            DatabaseType.POSTGRESQL, change.getRelation().getFromTableName(), change.getOrdinal());
    String onDelete = onDeleteClause(change.getRelation());
    writeGuardedAddConstraint(
        w,
        change.getRelation().getFromTableName(),
        fkName,
        "ALTER TABLE "
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
            + onDelete
            + ";");
  }

  @Override
  protected void generateDropRelation(DropRelationChange change) {
    PrintWriter w = options.getWriter();
    String fkName =
        Naming.foreignKeyName(
            DatabaseType.POSTGRESQL, change.getRelation().getFromTableName(), change.getOrdinal());
    w.println(
        "ALTER TABLE "
            + change.getRelation().getFromTableName()
            + " DROP CONSTRAINT IF EXISTS "
            + fkName);
    w.print(options.getStatementSeparator());
    w.println();
  }

  /**
   * Postgres has no {@code ADD CONSTRAINT IF NOT EXISTS} (for CHECK, PRIMARY KEY, or FOREIGN KEY
   * constraints), so idempotency has to be expressed as a {@code pg_constraint} existence check
   * wrapped in a {@code DO} block instead of a plain clause.
   */
  private void writeGuardedAddConstraint(
      PrintWriter w, String tableName, String constraintName, String addConstraintSql) {
    w.println("DO $$");
    w.println("BEGIN");
    w.println(
        "  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = '"
            + constraintName
            + "' AND conrelid = '"
            + tableName
            + "'::regclass) THEN");
    w.println("    " + addConstraintSql);
    w.println("  END IF;");
    w.println("END $$");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddFunction(AddFunctionChange change) {
    if (change.getFunction().getDatabaseType() != DatabaseType.POSTGRESQL) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println(change.getFunction().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropFunction(DropFunctionChange change) {
    if (change.getDatabaseType() != DatabaseType.POSTGRESQL) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println("DROP FUNCTION IF EXISTS " + change.getFunctionName() + " CASCADE");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddProcedure(AddProcedureChange change) {
    if (change.getProcedure().getDatabaseType() != DatabaseType.POSTGRESQL) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println(change.getProcedure().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropProcedure(DropProcedureChange change) {
    if (change.getDatabaseType() != DatabaseType.POSTGRESQL) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println("DROP PROCEDURE IF EXISTS " + change.getProcedureName() + " CASCADE");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddView(AddViewChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "CREATE OR REPLACE VIEW "
            + change.getView().getName()
            + " AS "
            + change.getView().getSql());
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
}
