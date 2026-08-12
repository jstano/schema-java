package com.stano.schema.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Checks whether an index with a given name exists anywhere in the database by querying the SQL
 * Server {@code dbo.sysindexes} system catalog.
 *
 * <p>Tied to the SQL Server system catalog and is not portable across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#indexExists} or {@link DropIndexMigration},
 * inside a hand-written Flyway or Liquibase Java migration to conditionally create or drop an index
 * only if it does (or does not) already exist.
 */
public class IndexExistsMigration implements StatementAction<Boolean> {
  private final String indexName;

  /**
   * Creates a migration helper that checks for the existence of an index named {@code indexName}.
   *
   * @param indexName the name of the index to check for
   */
  public IndexExistsMigration(String indexName) {
    this.indexName = indexName;
  }

  /**
   * Queries {@code dbo.sysindexes} for a row whose name matches the index name passed to the
   * constructor.
   *
   * @param statement the JDBC statement used to run the existence query
   * @return {@code true} if a matching row is found, {@code false} otherwise
   * @throws MigrationException if the query throws a {@link SQLException}
   */
  @Override
  public Boolean execute(Statement statement) {
    try (ResultSet rs =
        statement.executeQuery(
            String.format("select name from dbo.sysindexes where name = '%s'", indexName))) {
      return rs.next();
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
