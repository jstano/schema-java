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

public abstract class MigrationGenerator {
  protected final MigrationGeneratorOptions options;

  protected MigrationGenerator(MigrationGeneratorOptions options) {
    this.options = options;
  }

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

  protected abstract void generateAddTable(AddTableChange change);

  protected abstract void generateDropTable(DropTableChange change);

  protected abstract void generateRenameTable(RenameTableChange change);

  protected abstract void generateAddColumn(AddColumnChange change);

  protected abstract void generateDropColumn(DropColumnChange change);

  protected abstract void generateRenameColumn(RenameColumnChange change);

  protected abstract void generateModifyColumn(ModifyColumnChange change);

  protected abstract void generateAddKey(AddKeyChange change);

  protected abstract void generateDropKey(DropKeyChange change);

  protected abstract void generateAddConstraint(AddConstraintChange change);

  protected abstract void generateDropConstraint(DropConstraintChange change);

  protected abstract void generateAddRelation(AddRelationChange change);

  protected abstract void generateDropRelation(DropRelationChange change);

  protected abstract void generateAddFunction(AddFunctionChange change);

  protected abstract void generateDropFunction(DropFunctionChange change);

  protected abstract void generateAddProcedure(AddProcedureChange change);

  protected abstract void generateDropProcedure(DropProcedureChange change);

  protected abstract void generateAddView(AddViewChange change);

  protected abstract void generateDropView(DropViewChange change);
}
