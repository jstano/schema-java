package com.stano.schema.migrations;

import java.sql.Statement;

/**
 * Drops a named constraint on a SQL Server table, but only if it currently exists.
 *
 * <p>Delegates the existence check to {@link ConstraintExistsMigration} (which queries the SQL
 * Server {@code dbo.sysobjects} system catalog) and, if the constraint is found, drops it with an
 * {@code alter table <table> drop constraint <constraint>} statement via {@link
 * ExecuteSQLMigration}. Tied to SQL Server and is not portable across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#dropTableConstraint}, inside a hand-written
 * Flyway or Liquibase Java migration to safely drop a constraint without failing if it was already
 * removed.
 */
public class DropTableConstraintMigration implements StatementAction<Void> {
  private final String tableName;
  private final String constraintName;

  /**
   * Creates a migration helper that drops the given constraint on the given table if it exists.
   *
   * @param tableName the name of the table the constraint belongs to
   * @param constraintName the name of the constraint to drop
   */
  public DropTableConstraintMigration(String tableName, String constraintName) {
    this.tableName = tableName;
    this.constraintName = constraintName;
  }

  /**
   * Drops the configured constraint if it exists; does nothing otherwise.
   *
   * @param statement the JDBC statement used to check for the constraint and drop it
   * @return always {@code null}
   * @throws MigrationException if the existence check or the drop statement fails
   */
  @Override
  public Void execute(Statement statement) {
    if (constraintExists(statement, constraintName)) {
      dropConstraint(statement, tableName, constraintName);
    }

    return null;
  }

  /**
   * Checks whether a constraint with the given name currently exists.
   *
   * @param statement the JDBC statement used to run the existence check
   * @param constraintName the name of the constraint to check for
   * @return {@code true} if the constraint exists, {@code false} otherwise
   */
  private boolean constraintExists(Statement statement, String constraintName) {
    return new ConstraintExistsMigration(constraintName).execute(statement);
  }

  /**
   * Drops the given constraint on the given table.
   *
   * @param statement the JDBC statement used to run the {@code alter table ... drop constraint ...}
   *     statement
   * @param tableName the name of the table the constraint belongs to
   * @param constraintName the name of the constraint to drop
   */
  private void dropConstraint(Statement statement, String tableName, String constraintName) {
    new ExecuteSQLMigration(
            String.format("alter table %s drop constraint %s", tableName, constraintName))
        .execute(statement);
  }
}
