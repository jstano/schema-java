package com.stano.schema.migrations;

import java.sql.Statement;

/**
 * Drops an index on a SQL Server table, but only if it currently exists.
 *
 * <p>Delegates the existence check to {@link IndexExistsMigration} (which queries the SQL Server
 * {@code dbo.sysindexes} system catalog) and, if the index is found, drops it with a {@code drop
 * index <table>.<index>} statement via {@link ExecuteSQLMigration}. Tied to SQL Server and is not
 * portable across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#dropIndex}, inside a hand-written Flyway or
 * Liquibase Java migration to safely drop an index without failing if it was already removed.
 */
public class DropIndexMigration implements StatementAction<Void> {
  private final String tableName;
  private final String indexName;

  /**
   * Creates a migration helper that drops the given index on the given table if it exists.
   *
   * @param tableName the name of the table the index belongs to
   * @param indexName the name of the index to drop
   */
  public DropIndexMigration(String tableName, String indexName) {
    this.tableName = tableName;
    this.indexName = indexName;
  }

  /**
   * Drops the configured index if it exists; does nothing otherwise.
   *
   * @param statement the JDBC statement used to check for the index and drop it
   * @return always {@code null}
   * @throws MigrationException if the existence check or the drop statement fails
   */
  @Override
  public Void execute(Statement statement) {
    if (indexExists(statement)) {
      dropIndex(statement);
    }

    return null;
  }

  /**
   * Checks whether the configured index currently exists.
   *
   * @param statement the JDBC statement used to run the existence check
   * @return {@code true} if the index exists, {@code false} otherwise
   */
  private Boolean indexExists(Statement statement) {
    return new IndexExistsMigration(indexName).execute(statement);
  }

  /**
   * Drops the configured index on the configured table.
   *
   * @param statement the JDBC statement used to run the {@code drop index} statement
   * @return always {@code null}
   */
  private Object dropIndex(Statement statement) {
    return new ExecuteSQLMigration(String.format("drop index %s.%s", tableName, indexName))
        .execute(statement);
  }
}
