package com.stano.schema.migrations;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * A unit of migration work performed against a JDBC {@link Statement}.
 *
 * <p>Implementations of this interface (e.g. {@link ExecuteSQLMigration}, {@link
 * IndexExistsMigration}, {@link DropIndexMigration}) encapsulate a single existence check or DDL
 * operation. They are typically run via {@link MigrationServices}, which is responsible for
 * creating and closing the {@link Statement} passed to {@link #execute}.
 *
 * @param <T> the type of value produced by executing this action, or {@link Void} for actions that
 *     do not return a value
 */
@FunctionalInterface
interface StatementAction<T> {
  /**
   * Performs this action using the given statement.
   *
   * @param statement the JDBC statement to use for executing queries or updates
   * @return the result of the action, or {@code null} for actions that do not produce a value
   * @throws SQLException if a database access error occurs while performing the action
   */
  T execute(Statement statement) throws SQLException;
}
