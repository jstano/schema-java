package com.stano.schema.genmigration.impl.h2;

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
import com.stano.schema.gensql.impl.h2.H2ColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.DatabaseType;
import java.io.PrintWriter;

public class H2MigrationGenerator extends MigrationGenerator {
  private final ColumnTypeMapper mapper;

  public H2MigrationGenerator(MigrationGeneratorOptions options) {
    super(options);
    this.mapper = new H2ColumnTypeMapper(BooleanMode.NATIVE, options.getSchema());
  }

  @Override
  protected void generateAddTable(AddTableChange change) {
    PrintWriter w = options.getWriter();
    w.println("CREATE TABLE " + change.getTableName() + " ()");
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
    w.println("ALTER TABLE " + change.getOldName() + " RENAME TO " + change.getNewName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateRenameColumn(RenameColumnChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "ALTER TABLE "
            + change.getTableName()
            + " ALTER COLUMN "
            + change.getOldName()
            + " RENAME TO "
            + change.getNewName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddColumn(AddColumnChange change) {
    PrintWriter w = options.getWriter();
    Column col = change.getColumn();
    StringBuilder sb = new StringBuilder();
    sb.append("ALTER TABLE ")
        .append(change.getTableName())
        .append(" ADD COLUMN ")
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
      w.println(
          "ALTER TABLE "
              + change.getTableName()
              + " ALTER COLUMN "
              + col.getName()
              + " SET NOT NULL");
      w.print(options.getStatementSeparator());
      w.println();
    }
  }

  @Override
  protected void generateDropColumn(DropColumnChange change) {
    PrintWriter w = options.getWriter();
    w.println("ALTER TABLE " + change.getTableName() + " DROP COLUMN " + change.getColumnName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateModifyColumn(ModifyColumnChange change) {
    PrintWriter w = options.getWriter();
    Column newCol = change.getNewColumn();
    String tableName = change.getTableName();
    String colName = newCol.getName();

    w.println("ALTER TABLE " + tableName + " DROP COLUMN " + colName);
    w.print(options.getStatementSeparator());
    w.println();

    StringBuilder sb = new StringBuilder();
    sb.append("ALTER TABLE ")
        .append(tableName)
        .append(" ADD COLUMN ")
        .append(colName)
        .append(" ")
        .append(mapper.toSqlType(newCol));
    if (newCol.isRequired()) {
      sb.append(" NOT NULL");
    }
    if (newCol.getDefaultConstraint() != null) {
      sb.append(" DEFAULT ").append(newCol.getDefaultConstraint());
    }
    w.println(sb.toString());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddKey(AddKeyChange change) {
    PrintWriter w = options.getWriter();
    switch (change.getKey().getType()) {
      case PRIMARY:
        w.println(
            "ALTER TABLE "
                + change.getTableName()
                + " ADD PRIMARY KEY ("
                + change.getKey().getColumnsAsString()
                + ")");
        break;
      case UNIQUE:
      case INDEX:
        String indexName =
            "idx_"
                + change.getTableName()
                + "_"
                + change.getKey().getColumnsAsString().replace(",", "_");
        String unique =
            change.getKey().getType() == com.stano.schema.model.KeyType.UNIQUE ? "UNIQUE " : "";
        w.println(
            "CREATE "
                + unique
                + "INDEX "
                + indexName
                + " ON "
                + change.getTableName()
                + " ("
                + change.getKey().getColumnsAsString()
                + ")");
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
        w.println(
            "ALTER TABLE "
                + change.getTableName()
                + " DROP CONSTRAINT "
                + change.getTableName()
                + "_pkey");
        break;
      case UNIQUE:
      case INDEX:
        String indexName =
            "idx_"
                + change.getTableName()
                + "_"
                + change.getKey().getColumnsAsString().replace(",", "_");
        w.println("DROP INDEX IF EXISTS " + indexName);
        break;
    }
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddConstraint(AddConstraintChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "ALTER TABLE "
            + change.getTableName()
            + " ADD CONSTRAINT "
            + change.getConstraint().getName()
            + " CHECK ("
            + change.getConstraint().getSql()
            + ")");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropConstraint(DropConstraintChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "ALTER TABLE " + change.getTableName() + " DROP CONSTRAINT " + change.getConstraintName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddRelation(AddRelationChange change) {
    PrintWriter w = options.getWriter();
    String fkName =
        "fk_"
            + change.getRelation().getFromTableName()
            + "_"
            + change.getRelation().getFromColumnName();
    String onDelete =
        change.getRelation().getType() == com.stano.schema.model.RelationType.CASCADE
            ? " ON DELETE CASCADE"
            : "";
    w.println(
        "ALTER TABLE "
            + change.getRelation().getFromTableName()
            + " ADD CONSTRAINT "
            + fkName
            + " FOREIGN KEY ("
            + change.getRelation().getFromColumnName()
            + ") REFERENCES "
            + change.getRelation().getToTableName()
            + "("
            + change.getRelation().getToColumnName()
            + ")"
            + onDelete);
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropRelation(DropRelationChange change) {
    PrintWriter w = options.getWriter();
    String fkName =
        "fk_"
            + change.getRelation().getFromTableName()
            + "_"
            + change.getRelation().getFromColumnName();
    w.println(
        "ALTER TABLE " + change.getRelation().getFromTableName() + " DROP CONSTRAINT " + fkName);
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddFunction(AddFunctionChange change) {
    if (change.getFunction().getDatabaseType() != DatabaseType.H2) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println(change.getFunction().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropFunction(DropFunctionChange change) {
    if (change.getDatabaseType() != DatabaseType.H2) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println("DROP FUNCTION IF EXISTS " + change.getFunctionName());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateAddProcedure(AddProcedureChange change) {
    if (change.getProcedure().getDatabaseType() != DatabaseType.H2) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println(change.getProcedure().getSql());
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateDropProcedure(DropProcedureChange change) {
    if (change.getDatabaseType() != DatabaseType.H2) {
      return;
    }
    PrintWriter w = options.getWriter();
    w.println("DROP PROCEDURE IF EXISTS " + change.getProcedureName());
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
}
