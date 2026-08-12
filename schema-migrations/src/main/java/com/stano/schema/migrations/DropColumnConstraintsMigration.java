package com.stano.schema.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Drops all {@code CHECK} and {@code DEFAULT} constraints defined on a specific column of a SQL
 * Server table.
 *
 * <p>Uses the SQL Server {@code sp_helpconstraint} system stored procedure to enumerate the
 * constraints defined on the table, collects every result row whose constraint type text matches
 * either {@code "CHECK on column <column>"} or {@code "DEFAULT on column <column>"}, and drops each
 * matching constraint with an {@code alter table ... drop constraint ...} statement. Unlike {@link
 * DropColumnCheckConstraintMigration}, all matching constraints are dropped, not just the first.
 * Tied to SQL Server and is not portable across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#dropColumnConstraints}, inside a hand-written
 * Flyway or Liquibase Java migration that needs to remove a column's {@code CHECK} and {@code
 * DEFAULT} constraints before altering the column's type or dropping the column.
 */
public class DropColumnConstraintsMigration implements StatementAction<Void> {
  private final String table;
  private final String column;

  /**
   * Creates a migration helper that drops the {@code CHECK} and {@code DEFAULT} constraints on a
   * given column of a given table.
   *
   * @param table the name of the table that owns the column
   * @param column the name of the column whose {@code CHECK} and {@code DEFAULT} constraints should
   *     be dropped
   */
  public DropColumnConstraintsMigration(String table, String column) {
    this.table = table;
    this.column = column;
  }

  /**
   * Looks up the constraints on the configured table via {@code sp_helpconstraint}, collects every
   * {@code CHECK} or {@code DEFAULT} constraint found on the configured column, and drops each of
   * them.
   *
   * @param statement the JDBC statement used to run {@code sp_helpconstraint} and the {@code alter
   *     table ... drop constraint ...} statements
   * @return always {@code null}
   * @throws MigrationException if any of the JDBC calls throw a {@link SQLException}
   */
  @Override
  public Void execute(Statement statement) {
    try {
      statement.execute(String.format("exec sp_helpconstraint %s", table));

      ResultSet rs = statement.getResultSet();
      rs.close();

      List<String> constraints = new ArrayList<>();

      if (statement.getMoreResults()) {
        rs = statement.getResultSet();

        try {
          String checkConstraintText = String.format("CHECK on column %s", column);
          String defaultConstraintText = String.format("DEFAULT on column %s", column);

          while (rs.next()) {
            String constraintType = rs.getString(1);
            String constraintName = rs.getString(2);

            if (constraintType.equalsIgnoreCase(checkConstraintText)) {
              constraints.add(constraintName);
            } else if (constraintType.equalsIgnoreCase(defaultConstraintText)) {
              constraints.add(constraintName);
            }
          }
        } finally {
          rs.close();
        }
      }

      for (String constraint : constraints) {
        statement.execute("alter table " + table + " drop constraint " + constraint);
      }

      return null;
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
