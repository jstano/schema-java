package com.stano.schema.genmigration.impl.common;

import com.stano.schema.diff.SchemaChange;
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
import com.stano.schema.gensql.impl.common.ColumnConstraintGenerator;
import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorFactory;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Abstract base class for dialect-specific migration SQL generators (implemented per-database under
 * {@code impl.h2}, {@code impl.postgresql}, and {@code impl.sqlserver}).
 *
 * <p>{@link #generate()} is the template method: it iterates the {@link SchemaChange}s carried by
 * the configured {@link MigrationGeneratorOptions#getChangeSet()} and, for each one, dispatches to
 * the matching {@code generateAdd*}/{@code generateDrop*}/{@code generateRename*}/{@code
 * generateModify*} hook based on the change's concrete {@link SchemaChange} subtype (an {@code
 * instanceof} chain in the private {@code generateChange} method). Subclasses implement each hook
 * to emit the dialect-specific SQL for that kind of change, writing to {@link
 * MigrationGeneratorOptions#getWriter()}.
 *
 * <p>As a special case, when dispatching a {@link DropColumnChange} that carries rename candidates,
 * this class first writes a {@code -- TODO: possible rename?} comment (with the equivalent {@code
 * ALTER TABLE ... RENAME COLUMN} statement) before delegating to {@link
 * #generateDropColumn(DropColumnChange)}.
 */
public abstract class MigrationGenerator {
  protected final MigrationGeneratorOptions options;

  /**
   * Creates a new generator bound to the given options.
   *
   * @param options the changeset, writer, database type, and related settings to generate SQL with
   */
  protected MigrationGenerator(MigrationGeneratorOptions options) {
    this.options = options;
  }

  /**
   * Generates migration SQL for every change in the configured changeset, dispatching each one to
   * the matching {@code generate*} hook, then flushes the destination writer.
   */
  public void generate() {
    PrintWriter writer = options.getWriter();
    try {
      for (SchemaChange change : options.getChangeSet().getChanges()) {
        generateChange(change);
      }
    } finally {
      writer.flush();
    }
  }

  private void generateChange(SchemaChange change) {
    if (change instanceof AddTableChange c) {
      generateAddTable(c);
    } else if (change instanceof DropTableChange c) {
      generateDropTable(c);
    } else if (change instanceof RenameTableChange c) {
      generateRenameTable(c);
    } else if (change instanceof AddColumnChange c) {
      generateAddColumn(c);
    } else if (change instanceof DropColumnChange c) {
      if (!c.getRenameCandidates().isEmpty()) {
        PrintWriter w = options.getWriter();
        for (String candidate : c.getRenameCandidates()) {
          w.println("-- TODO: possible rename? Consider replacing the DROP + ADD below with:");
          w.println(
              "--   ALTER TABLE "
                  + c.getTableName()
                  + " RENAME COLUMN "
                  + c.getColumnName()
                  + " TO "
                  + candidate
                  + ";");
        }
      }
      generateDropColumn(c);
    } else if (change instanceof RenameColumnChange c) {
      generateRenameColumn(c);
    } else if (change instanceof ModifyColumnChange c) {
      generateModifyColumn(c);
    } else if (change instanceof AddKeyChange c) {
      generateAddKey(c);
    } else if (change instanceof DropKeyChange c) {
      generateDropKey(c);
    } else if (change instanceof AddConstraintChange c) {
      if (appliesToThisDialect(c.getConstraint().getDatabaseType())) {
        generateAddConstraint(c);
      }
    } else if (change instanceof DropConstraintChange c) {
      if (appliesToThisDialect(c.getDatabaseType())) {
        generateDropConstraint(c);
      }
    } else if (change instanceof AddRelationChange c) {
      // DONOTHING relations have no foreign key constraint to add (or, below, to drop).
      if (c.getRelation().createsForeignKeyConstraint()) {
        generateAddRelation(c);
      }
    } else if (change instanceof DropRelationChange c) {
      if (c.getRelation().createsForeignKeyConstraint()) {
        generateDropRelation(c);
      }
    } else if (change instanceof AddFunctionChange c) {
      generateAddFunction(c);
    } else if (change instanceof DropFunctionChange c) {
      generateDropFunction(c);
    } else if (change instanceof AddProcedureChange c) {
      generateAddProcedure(c);
    } else if (change instanceof DropProcedureChange c) {
      generateDropProcedure(c);
    } else if (change instanceof AddViewChange c) {
      if (appliesToThisDialect(c.getView().getDatabaseType())) {
        generateAddView(c);
      }
    } else if (change instanceof DropViewChange c) {
      if (appliesToThisDialect(c.getDatabaseType())) {
        generateDropView(c);
      }
    }
  }

  /**
   * Returns whether an object restricted to {@code objectDatabaseType} applies to the dialect this
   * generator produces SQL for - {@code true} if {@code objectDatabaseType} is {@code null} (the
   * object applies to every dialect) or matches {@link
   * MigrationGeneratorOptions#getDatabaseType()}.
   */
  private boolean appliesToThisDialect(com.stano.schema.model.DatabaseType objectDatabaseType) {
    return objectDatabaseType == null || objectDatabaseType == options.getDatabaseType();
  }

  /**
   * Generates the SQL statement(s) to create a new table.
   *
   * @param change the table addition to generate SQL for
   */
  protected abstract void generateAddTable(AddTableChange change);

  /**
   * Generates the SQL statement(s) to drop an existing table.
   *
   * @param change the table removal to generate SQL for
   */
  protected abstract void generateDropTable(DropTableChange change);

  /**
   * Generates the SQL statement(s) to rename an existing table.
   *
   * @param change the table rename to generate SQL for
   */
  protected abstract void generateRenameTable(RenameTableChange change);

  /**
   * Generates the SQL statement(s) to add a new column to an existing table.
   *
   * @param change the column addition to generate SQL for
   */
  protected abstract void generateAddColumn(AddColumnChange change);

  /**
   * Generates the SQL statement(s) to drop an existing column from a table.
   *
   * @param change the column removal to generate SQL for
   */
  protected abstract void generateDropColumn(DropColumnChange change);

  /**
   * Generates the SQL statement(s) to rename an existing column.
   *
   * @param change the column rename to generate SQL for
   */
  protected abstract void generateRenameColumn(RenameColumnChange change);

  /**
   * Generates the SQL statement(s) to modify an existing column's definition (e.g. type,
   * nullability, or default).
   *
   * @param change the column modification to generate SQL for
   */
  protected abstract void generateModifyColumn(ModifyColumnChange change);

  /**
   * Generates the SQL statement(s) to add a new key (primary or unique) to a table.
   *
   * @param change the key addition to generate SQL for
   */
  protected abstract void generateAddKey(AddKeyChange change);

  /**
   * Generates the SQL statement(s) to drop an existing key from a table.
   *
   * @param change the key removal to generate SQL for
   */
  protected abstract void generateDropKey(DropKeyChange change);

  /**
   * Generates the SQL statement(s) to add a new constraint to a table.
   *
   * @param change the constraint addition to generate SQL for
   */
  protected abstract void generateAddConstraint(AddConstraintChange change);

  /**
   * Generates the SQL statement(s) to drop an existing constraint from a table.
   *
   * @param change the constraint removal to generate SQL for
   */
  protected abstract void generateDropConstraint(DropConstraintChange change);

  /**
   * Generates the SQL statement(s) to add a new foreign-key relation between tables.
   *
   * @param change the relation addition to generate SQL for
   */
  protected abstract void generateAddRelation(AddRelationChange change);

  /**
   * Generates the SQL statement(s) to drop an existing foreign-key relation between tables.
   *
   * @param change the relation removal to generate SQL for
   */
  protected abstract void generateDropRelation(DropRelationChange change);

  /**
   * Generates the SQL statement(s) to create a new stored function.
   *
   * @param change the function addition to generate SQL for
   */
  protected abstract void generateAddFunction(AddFunctionChange change);

  /**
   * Generates the SQL statement(s) to drop an existing stored function.
   *
   * @param change the function removal to generate SQL for
   */
  protected abstract void generateDropFunction(DropFunctionChange change);

  /**
   * Generates the SQL statement(s) to create a new stored procedure.
   *
   * @param change the procedure addition to generate SQL for
   */
  protected abstract void generateAddProcedure(AddProcedureChange change);

  /**
   * Generates the SQL statement(s) to drop an existing stored procedure.
   *
   * @param change the procedure removal to generate SQL for
   */
  protected abstract void generateDropProcedure(DropProcedureChange change);

  /**
   * Generates the SQL statement(s) to create a new view.
   *
   * @param change the view addition to generate SQL for
   */
  protected abstract void generateAddView(AddViewChange change);

  /**
   * Generates the SQL statement(s) to drop an existing view.
   *
   * @param change the view removal to generate SQL for
   */
  protected abstract void generateDropView(DropViewChange change);

  /**
   * Returns the {@code ON DELETE} clause (with a leading space) for a relation, or an empty string
   * for {@code ENFORCE}, which is the database default of {@code NO ACTION}. {@code DONOTHING}
   * relations get no foreign key at all (see {@link Relation#createsForeignKeyConstraint()}), so
   * callers skip them before asking.
   */
  protected static String onDeleteClause(Relation relation) {
    return switch (relation.getType()) {
      case CASCADE -> " ON DELETE CASCADE";
      case SETNULL -> " ON DELETE SET NULL";
      case ENFORCE, DONOTHING -> "";
    };
  }

  /**
   * Writes the comment block that explains why a migration halts when a required column is added
   * with no default: existing rows have no value for it, so the developer must either backfill or
   * confirm the table is empty by deleting the halt statement that follows.
   */
  protected final void writeBackfillWarning(String tableName, String columnName) {
    PrintWriter w = options.getWriter();
    w.println(
        "-- WARNING: '"
            + columnName
            + "' is being made NOT NULL with no default - existing rows have no value to backfill"
            + " it with automatically.");
    w.println(
        "--   1. If '"
            + tableName
            + "' has rows, add UPDATE statements above this line to give '"
            + columnName
            + "' a value, then delete the block below, OR");
    w.println("--   2. If '" + tableName + "' is empty, delete the block below to confirm that.");
  }

  /**
   * Writes the comment block that explains why a migration halts when a column's {@code generated}
   * expression changes (or it switches between plain and generated): there is no portable {@code
   * ALTER COLUMN} for it, and dropping and re-adding a plain column would lose data.
   */
  protected final void writeGeneratedChangeWarning(String tableName, String columnName) {
    PrintWriter w = options.getWriter();
    w.println(
        "-- WARNING: the generated expression of '"
            + tableName
            + "."
            + columnName
            + "' changed (or it switched between plain and generated).");
    w.println(
        "--   This cannot be altered in place. Drop and re-add the column manually (back up its"
            + " data first if it was a plain column), then delete the statement below.");
  }

  /** Returns whether a {@code ModifyColumn} changes the column's {@code generated} expression. */
  protected static boolean generatedChanged(Column oldColumn, Column newColumn) {
    return !java.util.Objects.equals(oldColumn.getGenerated(), newColumn.getGenerated());
  }

  private ColumnConstraintGenerator columnConstraintGenerator;

  /**
   * Returns the {@code check(...)} clause the real {@code CREATE TABLE} generator would attach to
   * this column (boolean {@code YesNo}/{@code YN} modes, an explicit {@code check} attribute, enum
   * value lists, and min/max bounds), or {@code null} if the column has none - delegating to the
   * dialect's own {@link ColumnConstraintGenerator} so an {@code AddColumn} migration enforces
   * exactly the same constraint a fresh install would, with no separate reimplementation to drift
   * out of sync.
   *
   * @param column the column to render a check constraint for
   * @return the check clause, or {@code null} if the column has no check constraint (or if these
   *     options carry no {@link Schema}, e.g. in tests that only exercise a single change)
   */
  protected final String getCheckConstraintSql(Column column) {
    if (options.getSchema() == null) {
      return null;
    }
    return getColumnConstraintGenerator().getCheckConstraintSQL(column);
  }

  /**
   * Returns the {@code ck_<table>_<column>_<hash>} name the real {@code CREATE TABLE} generator
   * would give this column's check constraint, so a migration-generated constraint matches what a
   * fresh install would produce.
   *
   * @param tableName the table the column belongs to
   * @param columnName the column the constraint is for
   * @return the constraint name
   */
  protected final String getCheckConstraintName(String tableName, String columnName) {
    return ColumnConstraintGenerator.constraintName(tableName, columnName);
  }

  /**
   * Builds (and caches) this generator's dialect-specific {@link ColumnConstraintGenerator}, using
   * an in-memory sink so it can be created without running a full {@code CREATE TABLE} generation.
   */
  private ColumnConstraintGenerator getColumnConstraintGenerator() {
    if (columnConstraintGenerator == null) {
      Schema schema = options.getSchema();
      SQLGeneratorOptions sqlGeneratorOptions =
          new SQLGeneratorOptions(
              schema,
              new PrintWriter(Writer.nullWriter()),
              options.getDatabaseType(),
              ForeignKeyMode.RELATIONS,
              schema.getBooleanMode(),
              OutputMode.ALL);
      SQLGenerator sqlGenerator = new SQLGeneratorFactory().createSQLGenerator(sqlGeneratorOptions);
      columnConstraintGenerator = sqlGenerator.getColumnConstraintGenerator();
    }
    return columnConstraintGenerator;
  }
}
