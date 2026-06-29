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
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGenerator;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.sqlserver.SQLServerColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.DatabaseType;
import java.io.PrintWriter;
import java.util.Objects;

public class SQLServerMigrationGenerator extends MigrationGenerator {
  private final ColumnTypeMapper mapper;

  public SQLServerMigrationGenerator(MigrationGeneratorOptions options) {
    super(options);
    this.mapper = new SQLServerColumnTypeMapper(BooleanMode.NATIVE, options.getSchema());
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
    w.println("EXEC sp_rename '" + change.getOldName() + "', '" + change.getNewName() + "'");
    w.print(options.getStatementSeparator());
    w.println();
  }

  @Override
  protected void generateRenameColumn(RenameColumnChange change) {
    PrintWriter w = options.getWriter();
    w.println(
        "EXEC sp_rename '"
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
    StringBuilder sb = new StringBuilder();
    sb.append("ALTER TABLE ")
        .append(change.getTableName())
        .append(" ADD ")
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
              + " "
              + mapper.toSqlType(col)
              + " NOT NULL");
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
        .append(" ")
        .append(mapper.toSqlType(newCol));
    if (newCol.isRequired()) {
      sb.append(" NOT NULL");
    }
    w.println(sb.toString());
    w.print(options.getStatementSeparator());
    w.println();

    if (!Objects.equals(oldCol.getDefaultConstraint(), newCol.getDefaultConstraint())) {
      if (newCol.getDefaultConstraint() != null) {
        w.println(
            "ALTER TABLE "
                + change.getTableName()
                + " ADD DEFAULT "
                + newCol.getDefaultConstraint()
                + " FOR "
                + newCol.getName());
        w.print(options.getStatementSeparator());
        w.println();
      } else {
        w.println(
            "-- TODO: DROP DEFAULT constraint on "
                + change.getTableName()
                + "."
                + newCol.getName());
        w.println();
      }
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
        w.println("DROP INDEX IF EXISTS " + indexName + " ON " + change.getTableName());
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
}
