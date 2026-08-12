package com.stano.schema.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Drops every trigger defined in the database by querying the SQL Server {@code dbo.sysobjects}
 * system catalog for objects of type {@code TR} and issuing a {@code drop trigger} statement for
 * each one found.
 *
 * <p>Tied to the SQL Server system catalog and is not portable across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#dropAllTriggers}, inside a hand-written Flyway or
 * Liquibase Java migration that needs to clear out all triggers before reapplying schema changes,
 * e.g. before bulk data loads or schema rebuilds.
 */
public class DropAllTriggersMigration implements StatementAction<Void> {
  /**
   * Looks up every trigger currently defined in the database and drops each one in turn.
   *
   * @param statement the JDBC statement used to query {@code dbo.sysobjects} and issue the {@code
   *     drop trigger} statements
   * @return always {@code null}
   * @throws MigrationException if looking up the trigger names or dropping a trigger throws a
   *     {@link SQLException}
   */
  @Override
  public Void execute(Statement statement) {
    try {
      for (String triggerName : loadTriggerNames(statement)) {
        statement.executeUpdate(String.format("drop trigger %s", triggerName));
      }

      return null;
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }

  /**
   * Queries {@code dbo.sysobjects} for the names of all objects of type {@code TR} (triggers).
   *
   * @param statement the JDBC statement used to run the query
   * @return the names of all triggers currently defined in the database
   * @throws SQLException if the query fails
   */
  private List<String> loadTriggerNames(Statement statement) throws SQLException {
    List<String> triggerNames = new ArrayList<>();

    try (ResultSet rs =
        statement.executeQuery("select name from dbo.sysobjects where type = 'TR'")) {
      while (rs.next()) {
        triggerNames.add(rs.getString("name"));
      }
    }

    return triggerNames;
  }
}
