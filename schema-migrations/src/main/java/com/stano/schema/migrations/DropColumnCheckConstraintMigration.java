package com.stano.schema.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Drops the {@code CHECK} constraint (if any) defined on a specific column of a SQL Server table.
 *
 * <p>Uses the SQL Server {@code sp_helpconstraint} system stored procedure to enumerate the
 * constraints defined on the table, finds the first result row whose constraint type text matches
 * {@code "CHECK on column <column>"}, and drops that constraint with an {@code alter table ... drop
 * constraint ...} statement. Only the first matching {@code CHECK} constraint is dropped. Tied to
 * SQL Server and is not portable across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#dropColumnCheckConstraint}, inside a hand-written
 * Flyway or Liquibase Java migration that needs to remove a column's {@code CHECK} constraint
 * before altering the column's type or dropping the column.
 */
public class DropColumnCheckConstraintMigration implements StatementAction<Object> {
  private final String table;
  private final String column;

  /**
   * Creates a migration helper that drops the {@code CHECK} constraint on a given column of a given
   * table.
   *
   * @param table the name of the table that owns the column
   * @param column the name of the column whose {@code CHECK} constraint should be dropped
   */
  public DropColumnCheckConstraintMigration(String table, String column) {
    this.table = table;
    this.column = column;
  }

  /**
   * Looks up the constraints on the configured table via {@code sp_helpconstraint} and drops the
   * first {@code CHECK} constraint found on the configured column, if any.
   *
   * @param statement the JDBC statement used to run {@code sp_helpconstraint} and the {@code alter
   *     table ... drop constraint ...} statement
   * @return always {@code null}
   * @throws MigrationException if any of the JDBC calls throw a {@link SQLException}
   */
  @Override
  public Object execute(Statement statement) {
    try {
      statement.execute(String.format("exec sp_helpconstraint %s", table));

      if (statement.getMoreResults()) {
        String checkConstraintText = String.format("CHECK on column %s", column);

        try (ResultSet rs = statement.getResultSet()) {
          while (rs.next()) {
            String constraintType = rs.getString(1);
            String constraintName = rs.getString(2);

            if (constraintType.equalsIgnoreCase(checkConstraintText)) {
              statement.execute("alter table " + table + " drop constraint " + constraintName);
              break;
            }
          }
        }
      }

      return null;
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
