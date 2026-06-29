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
import com.stano.schema.model.DatabaseType;
import java.io.PrintWriter;
import java.util.Objects;

public class PostgreSQLMigrationGenerator extends MigrationGenerator {
  private final ColumnTypeMapper mapper;

  public PostgreSQLMigrationGenerator(MigrationGeneratorOptions options) {
    super(options);
    this.mapper = new PostgreSQLColumnTypeMapper(BooleanMode.NATIVE, options.getSchema());
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
            + " RENAME COLUMN "
            + change.getOldName()
            + " TO "
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
    Column oldCol = change.getOldColumn();
    Column newCol = change.getNewColumn();
    StringBuilder sb = new StringBuilder();
    sb.append("ALTER TABLE ")
        .append(change.getTableName())
        .append(" ALTER COLUMN ")
        .append(newCol.getName())
        .append(" TYPE ")
        .append(mapper.toSqlType(newCol));
    w.println(sb.toString());
    w.print(options.getStatementSeparator());
    w.println();

    if (oldCol.isRequired() != newCol.isRequired()) {
      StringBuilder sb2 = new StringBuilder();
      sb2.append("ALTER TABLE ")
          .append(change.getTableName())
          .append(" ALTER COLUMN ")
          .append(newCol.getName());
      if (newCol.isRequired()) {
        sb2.append(" SET NOT NULL");
      } else {
        sb2.append(" DROP NOT NULL");
      }
      w.println(sb2.toString());
      w.print(options.getStatementSeparator());
      w.println();
    }

    if (!Objects.equals(oldCol.getDefaultConstraint(), newCol.getDefaultConstraint())) {
      StringBuilder sb3 = new StringBuilder();
      sb3.append("ALTER TABLE ")
          .append(change.getTableName())
          .append(" ALTER COLUMN ")
          .append(newCol.getName());
      if (newCol.getDefaultConstraint() != null) {
        sb3.append(" SET DEFAULT ").append(newCol.getDefaultConstraint());
      } else {
        sb3.append(" DROP DEFAULT");
      }
      w.println(sb3.toString());
      w.print(options.getStatementSeparator());
      w.println();
    }
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
