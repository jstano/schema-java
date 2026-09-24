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
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Naming;
import java.io.PrintWriter;
import java.util.Objects;
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
    StringBuilder sb = new StringBuilder();
    sb.append("IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('")
        .append(change.getTableName())
        .append("') AND name = '")
        .append(col.getName())
        .append("') ALTER TABLE ")
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

    String checkSql = getCheckConstraintSql(col);
    if (checkSql != null) {
      String constraintName = getCheckConstraintName(change.getTableName(), col.getName());
      w.println(
          "IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = '"
              + constraintName
              + "') ALTER TABLE "
              + change.getTableName()
              + " ADD CONSTRAINT "
              + constraintName
              + " "
              + checkSql);
      w.print(options.getStatementSeparator());
      w.println();
    }
  }

  @Override
  protected void generateDropColumn(DropColumnChange change) {
    PrintWriter w = options.getWriter();
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
    String onDelete =
        change.getRelation().getType() == com.stano.schema.model.RelationType.CASCADE
            ? " ON DELETE CASCADE"
            : "";
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
}
