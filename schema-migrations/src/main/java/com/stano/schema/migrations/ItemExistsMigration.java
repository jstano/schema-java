package com.stano.schema.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Checks whether an object of a given name and type exists in the database by querying the SQL
 * Server {@code dbo.sysobjects} system catalog.
 *
 * <p>The {@code type} parameter corresponds to the single- or two-character {@code dbo.sysobjects}
 * type code used by SQL Server (e.g. {@code "U"} for a user table, {@code "P"} for a stored
 * procedure, {@code "TR"} for a trigger). Tied to the SQL Server system catalog and is not portable
 * across database vendors.
 *
 * <p>Typically used, via {@link MigrationServices#itemExists}, inside a hand-written Flyway or
 * Liquibase Java migration to conditionally create or drop a database object only if it does (or
 * does not) already exist.
 */
public class ItemExistsMigration implements StatementAction<Boolean> {
  private final String name;
  private final String type;

  /**
   * Creates a migration helper that checks for the existence of an object named {@code name} with
   * the given {@code dbo.sysobjects} type code.
   *
   * @param name the name of the object to check for
   * @param type the SQL Server {@code dbo.sysobjects} type code of the object (e.g. {@code "U"} for
   *     a table, {@code "P"} for a procedure, {@code "TR"} for a trigger)
   */
  public ItemExistsMigration(String name, String type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Queries {@code dbo.sysobjects} for a row whose name and type match the values passed to the
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
            String.format(
                "select name from dbo.sysobjects where name = '%s' and type = '%s'", name, type))) {
      return rs.next();
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
