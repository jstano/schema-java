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
import com.stano.schema.diff.change.ModifyEnumTypeChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGenerator;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.h2.H2ColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Naming;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2MigrationGenerator extends MigrationGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(H2MigrationGenerator.class);

  private final ColumnTypeMapper mapper;

  public H2MigrationGenerator(MigrationGeneratorOptions options) {
    super(options);
    this.mapper = new H2ColumnTypeMapper(BooleanMode.NATIVE, options.getSchema());
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
    w.println(
        "ALTER TABLE "
            + change.getTableName()
            + " ALTER COLUMN IF EXISTS "
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
        .append(" ADD COLUMN IF NOT EXISTS ")
        .append(col.getName())
        .append(" ")
        .append(mapper.toSqlType(col));
    if (col.getGenerated() != null) {
      // Computed on read, so H2 has no STORED keyword; generated columns take no default.
      sb.append(" GENERATED ALWAYS AS (").append(col.getGenerated()).append(")");
      if (col.isRequired()) {
        sb.append(" NOT NULL");
      }
    } else if (col.getDefaultConstraint() != null) {
      sb.append(" DEFAULT ").append(col.getDefaultConstraint());
    }
    w.println(sb.toString());
    w.print(options.getStatementSeparator());
    w.println();

    if (col.isRequired() && col.getGenerated() == null) {
      w.println(
          "ALTER TABLE "
              + change.getTableName()
              + " ALTER COLUMN "
              + col.getName()
              + " SET NOT NULL");
      w.print(options.getStatementSeparator());
      w.println();
    }

    String checkSql = getCheckConstraintSql(col);
    if (checkSql != null) {
      w.println(
          "ALTER TABLE "
              + change.getTableName()
              + " ADD CONSTRAINT IF NOT EXISTS "
              + getCheckConstraintName(change.getTableName(), col.getName())
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
        "ALTER TABLE "
            + change.getTableName()
            + " DROP COLUMN IF EXISTS "
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
    String alter = "ALTER TABLE " + table + " ALTER COLUMN " + newCol.getName();

    String newType = alterColumnType(newCol);
    boolean typeChanged = !newType.equals(alterColumnType(oldCol));

    // The column's check clause is built from its type, check, min/max and enum attributes; when
    // any changed, the old constraint is stale, so it comes off first and the new one goes back on.
    boolean cycleCheck = typeChanged || checkAttributesChanged(oldCol, newCol);
    String checkName = getCheckConstraintName(table, newCol.getName());
    if (cycleCheck) {
      writeStatement("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + checkName);
    }

    if (typeChanged) {
      writeStatement(alter + " SET DATA TYPE " + newType);
    }

    if (oldCol.isRequired() != newCol.isRequired()) {
      writeStatement(alter + (newCol.isRequired() ? " SET NOT NULL" : " DROP NOT NULL"));
    }

    if (!Objects.equals(oldCol.getDefaultConstraint(), newCol.getDefaultConstraint())) {
      writeStatement(
          alter
              + (newCol.getDefaultConstraint() != null
                  ? " SET DEFAULT " + newCol.getDefaultConstraint()
                  : " DROP DEFAULT"));
    }

    if (cycleCheck) {
      String checkSql = getCheckConstraintSql(newCol);
      if (checkSql != null) {
        writeStatement(
            "ALTER TABLE " + table + " ADD CONSTRAINT IF NOT EXISTS " + checkName + " " + checkSql);
      }
    }
    w.flush();
  }

  /**
   * The type to hand {@code SET DATA TYPE}. An identity column's mapped type carries the identity
   * clause, which {@code SET DATA TYPE} rejects, so it is reduced to the bare integer type.
   */
  private String alterColumnType(Column column) {
    if (column.getType() == ColumnType.SEQUENCE) {
      return "integer";
    }
    if (column.getType() == ColumnType.LONGSEQUENCE) {
      return "bigint";
    }
    return mapper.toSqlType(column);
  }

  private static boolean checkAttributesChanged(Column oldCol, Column newCol) {
    return !Objects.equals(oldCol.getCheckConstraint(), newCol.getCheckConstraint())
        || !Objects.equals(oldCol.getMinValue(), newCol.getMinValue())
        || !Objects.equals(oldCol.getMaxValue(), newCol.getMaxValue())
        || !Objects.equals(oldCol.getEnumType(), newCol.getEnumType());
  }

  private void writeStatement(String sql) {
    PrintWriter w = options.getWriter();
    w.println(sql);
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
                + " ADD CONSTRAINT IF NOT EXISTS "
                + Naming.primaryKeyName(DatabaseType.H2, change.getTableName())
                + " PRIMARY KEY ("
                + change.getKey().getColumnsAsString()
                + ")");
        break;
      case UNIQUE:
        w.println(
            "CREATE UNIQUE INDEX IF NOT EXISTS "
                + Naming.uniqueKeyName(DatabaseType.H2, change.getTableName(), change.getOrdinal())
                + " ON "
                + change.getTableName()
                + " ("
                + change.getKey().getColumnsAsString()
                + ")");
        break;
      case INDEX:
        if (change.getKey().getFilter() != null) {
          LOGGER.warn("H2 does not support partial/filtered indexes; ignoring where on index");
        }
        w.println(
            "CREATE "
                + (change.getKey().isUnique() ? "UNIQUE " : "")
                + "INDEX IF NOT EXISTS "
                + Naming.indexName(DatabaseType.H2, change.getTableName(), change.getOrdinal())
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
                + " DROP CONSTRAINT IF EXISTS "
                + Naming.primaryKeyName(DatabaseType.H2, change.getTableName()));
        break;
      case UNIQUE:
        w.println(
            "DROP INDEX IF EXISTS "
                + Naming.uniqueKeyName(
                    DatabaseType.H2, change.getTableName(), change.getOrdinal()));
        break;
      case INDEX:
        w.println(
            "DROP INDEX IF EXISTS "
                + Naming.indexName(DatabaseType.H2, change.getTableName(), change.getOrdinal()));
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
            + " ADD CONSTRAINT IF NOT EXISTS "
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
            DatabaseType.H2, change.getRelation().getFromTableName(), change.getOrdinal());
    String onDelete = onDeleteClause(change.getRelation());
    w.println(
        "ALTER TABLE "
            + change.getRelation().getFromTableName()
            + " ADD CONSTRAINT IF NOT EXISTS "
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
            DatabaseType.H2, change.getRelation().getFromTableName(), change.getOrdinal());
    w.println(
        "ALTER TABLE "
            + change.getRelation().getFromTableName()
            + " DROP CONSTRAINT IF EXISTS "
            + fkName);
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

  /**
   * H2 has no named enum type: each column carries its own inline {@code ENUM(...)} type, and there
   * is no safe in-place change that preserves a column's other attributes, so the affected columns
   * are listed in a comment for the developer to alter by hand.
   */
  @Override
  protected void generateModifyEnumType(ModifyEnumTypeChange change) {
    PrintWriter w = options.getWriter();
    String name = change.getNewEnumType().getName();
    w.println(
        "-- TODO: enum type '"
            + name
            + "' changed; H2 stores it inline per column, so alter these columns manually:");
    if (options.getSchema() != null) {
      for (var table : options.getSchema().getTables()) {
        for (var column : table.getColumns()) {
          if (name.equalsIgnoreCase(column.getEnumType())) {
            w.println("--   " + table.getName() + "." + column.getName());
          }
        }
      }
    }
    w.println();
  }
}
