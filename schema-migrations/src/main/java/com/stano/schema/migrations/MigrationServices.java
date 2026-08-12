package com.stano.schema.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Facade over the individual {@link StatementAction} implementations and existence-check helpers in
 * this package, providing a single convenient entry point for common migration operations.
 *
 * <p>Each method obtains (or reuses, in the case of the metadata-based checks) a {@link
 * Connection}/{@link Statement}, delegates to the corresponding helper class, and translates any
 * {@link SQLException} into an unchecked {@link MigrationException}.
 *
 * <p>Note that {@link #indexExists}, {@link #dropIndex}, {@link #dropColumnCheckConstraint}, {@link
 * #dropColumnConstraints}, {@link #dropTableConstraint}, {@link #constraintExists}, {@link
 * #itemExists}, and {@link #dropAllTriggers} rely on SQL Server-specific system catalogs ( {@code
 * dbo.sysobjects}, {@code dbo.sysindexes}) and the {@code sp_helpconstraint} stored procedure, and
 * are therefore only usable against SQL Server. {@link #tableExists} and {@link #columnExists} use
 * standard JDBC {@link java.sql.DatabaseMetaData} and are portable across database vendors.
 *
 * <p>Intended to be instantiated once and used directly from hand-written Flyway or Liquibase Java
 * migration classes to conditionally create, drop, or alter schema objects.
 */
public class MigrationServices {
  /**
   * Checks whether an index with the given name exists.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param indexName the name of the index to check for
   * @return {@code true} if the index exists, {@code false} otherwise
   * @throws MigrationException if the underlying query throws a {@link SQLException}
   */
  public boolean indexExists(Connection connection, String indexName) {
    return executeWithStatement(connection, new IndexExistsMigration(indexName));
  }

  /**
   * Drops the given index on the given table, but only if it currently exists.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param tableName the name of the table the index belongs to
   * @param indexName the name of the index to drop
   * @throws MigrationException if the existence check or the drop statement throws a {@link
   *     SQLException}
   */
  public void dropIndex(Connection connection, String tableName, String indexName) {
    executeWithStatement(connection, new DropIndexMigration(tableName, indexName));
  }

  /**
   * Checks whether a table with the given name exists.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param name the name of the table to check for
   * @return {@code true} if the table exists, {@code false} otherwise
   * @throws MigrationException if the underlying metadata query throws a {@link SQLException}
   */
  public boolean tableExists(Connection connection, String name) {
    return new TableExistsMigration(connection, name).tableExists();
  }

  /**
   * Drops the {@code CHECK} constraint (if any) defined on the given column of the given table.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param tableName the name of the table that owns the column
   * @param columnName the name of the column whose {@code CHECK} constraint should be dropped
   * @throws MigrationException if the underlying JDBC calls throw a {@link SQLException}
   */
  public void dropColumnCheckConstraint(
      Connection connection, String tableName, String columnName) {
    executeWithStatement(connection, new DropColumnCheckConstraintMigration(tableName, columnName));
  }

  /**
   * Drops all {@code CHECK} and {@code DEFAULT} constraints defined on the given column of the
   * given table.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param tableName the name of the table that owns the column
   * @param columnName the name of the column whose {@code CHECK} and {@code DEFAULT} constraints
   *     should be dropped
   * @throws MigrationException if the underlying JDBC calls throw a {@link SQLException}
   */
  public void dropColumnConstraints(Connection connection, String tableName, String columnName) {
    executeWithStatement(connection, new DropColumnConstraintsMigration(tableName, columnName));
  }

  /**
   * Drops the given constraint on the given table, but only if it currently exists.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param tableName the name of the table the constraint belongs to
   * @param constraintName the name of the constraint to drop
   * @throws MigrationException if the existence check or the drop statement throws a {@link
   *     SQLException}
   */
  public void dropTableConstraint(Connection connection, String tableName, String constraintName) {
    executeWithStatement(connection, new DropTableConstraintMigration(tableName, constraintName));
  }

  /**
   * Checks whether a constraint (or other {@code dbo.sysobjects} entry) with the given name exists.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param constraintName the name of the constraint to check for
   * @return {@code true} if the constraint exists, {@code false} otherwise
   * @throws MigrationException if the underlying query throws a {@link SQLException}
   */
  public boolean constraintExists(Connection connection, String constraintName) {
    return executeWithStatement(connection, new ConstraintExistsMigration(constraintName));
  }

  /**
   * Checks whether the given column exists on the given table.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param tableName the name of the table to check
   * @param columnName the name of the column to check for
   * @return {@code true} if the column exists, {@code false} otherwise
   * @throws MigrationException if the underlying metadata query throws a {@link SQLException}
   */
  public boolean columnExists(Connection connection, String tableName, String columnName) {
    return new ColumnExistsMigration(connection, tableName, columnName).columnExists();
  }

  /**
   * Checks whether an object with the given name and {@code dbo.sysobjects} type code exists.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param name the name of the object to check for
   * @param type the SQL Server {@code dbo.sysobjects} type code of the object (e.g. {@code "U"} for
   *     a table, {@code "P"} for a procedure, {@code "TR"} for a trigger)
   * @return {@code true} if a matching object exists, {@code false} otherwise
   * @throws MigrationException if the underlying query throws a {@link SQLException}
   */
  public boolean itemExists(Connection connection, String name, String type) {
    return executeWithStatement(connection, new ItemExistsMigration(name, type));
  }

  /**
   * Executes an arbitrary SQL update statement.
   *
   * @param connection the JDBC connection to the database being migrated
   * @param sql the SQL statement to execute
   * @throws MigrationException if executing the statement throws a {@link SQLException}
   */
  public void executeSQL(Connection connection, String sql) {
    executeWithStatement(connection, new ExecuteSQLMigration(sql));
  }

  /**
   * Drops every trigger currently defined in the database.
   *
   * @param connection the JDBC connection to the database being migrated
   * @throws MigrationException if looking up or dropping a trigger throws a {@link SQLException}
   */
  public void dropAllTriggers(Connection connection) {
    executeWithStatement(connection, new DropAllTriggersMigration());
  }

  /**
   * Creates a JDBC {@link Statement} from the given connection, runs the given action with it, and
   * ensures the statement is closed afterward.
   *
   * @param connection the JDBC connection to create the statement from
   * @param action the migration action to run with the statement
   * @param <T> the type of value returned by the action
   * @return the value returned by {@code action.execute(statement)}
   * @throws MigrationException if creating the statement or running the action throws a {@link
   *     SQLException}
   */
  private <T> T executeWithStatement(Connection connection, StatementAction<T> action) {
    try (Statement statement = connection.createStatement()) {
      return action.execute(statement);
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
