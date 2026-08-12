package com.stano.schema.migrations;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes an arbitrary SQL update statement (DDL or DML) against the database.
 *
 * <p>This is the generic building block used by several of the other {@link StatementAction}
 * implementations in this package (e.g. {@link DropIndexMigration}, {@link
 * DropTableConstraintMigration}) to actually run the statement they compute.
 *
 * <p>Typically used, via {@link MigrationServices#executeSQL}, inside a hand-written Flyway or
 * Liquibase Java migration to run a one-off SQL statement that is not otherwise covered by this
 * package's more specific helpers.
 */
public class ExecuteSQLMigration implements StatementAction<Void> {
  private final String sql;

  /**
   * Creates a migration helper that executes the given SQL statement.
   *
   * @param sql the SQL statement to execute via {@link Statement#executeUpdate(String)}
   */
  public ExecuteSQLMigration(String sql) {
    this.sql = sql;
  }

  /**
   * Executes the configured SQL statement.
   *
   * @param statement the JDBC statement used to run the SQL
   * @return always {@code null}
   * @throws MigrationException if executing the statement throws a {@link SQLException}
   */
  @Override
  public Void execute(Statement statement) {
    try {
      statement.executeUpdate(sql);

      return null;
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
