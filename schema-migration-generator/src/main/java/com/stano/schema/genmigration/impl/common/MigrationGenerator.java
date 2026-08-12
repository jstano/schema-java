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
import java.io.PrintWriter;

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
      generateAddConstraint(c);
    } else if (change instanceof DropConstraintChange c) {
      generateDropConstraint(c);
    } else if (change instanceof AddRelationChange c) {
      generateAddRelation(c);
    } else if (change instanceof DropRelationChange c) {
      generateDropRelation(c);
    } else if (change instanceof AddFunctionChange c) {
      generateAddFunction(c);
    } else if (change instanceof DropFunctionChange c) {
      generateDropFunction(c);
    } else if (change instanceof AddProcedureChange c) {
      generateAddProcedure(c);
    } else if (change instanceof DropProcedureChange c) {
      generateDropProcedure(c);
    } else if (change instanceof AddViewChange c) {
      generateAddView(c);
    } else if (change instanceof DropViewChange c) {
      generateDropView(c);
    }
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
}
