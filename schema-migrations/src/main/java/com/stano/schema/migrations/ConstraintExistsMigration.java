package com.stano.schema.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Checks whether a constraint (or any other object tracked in the SQL Server {@code dbo.sysobjects}
 * system catalog) with a given name exists in the database.
 *
 * <p>Executed via a {@link Statement} rather than JDBC metadata, so this is tied to the SQL Server
 * system catalog and is not portable across database vendors.
 *
 * <p>Typically used, together with {@link MigrationServices#constraintExists}, inside a
 * hand-written Flyway or Liquibase Java migration to conditionally drop or create a constraint only
 * if it does (or does not) already exist.
 */
public class ConstraintExistsMigration implements StatementAction<Boolean> {
  private final String constraintName;

  /**
   * Creates a migration helper that checks for the existence of an object named {@code
   * constraintName} in {@code dbo.sysobjects}.
   *
   * @param constraintName the name of the constraint (or other {@code sysobjects} entry) to check
   *     for
   */
  public ConstraintExistsMigration(String constraintName) {
    this.constraintName = constraintName;
  }

  /**
   * Queries {@code dbo.sysobjects} for a row whose name matches the constraint name passed to the
   * constructor.
   *
   * @param statement the JDBC statement used to run the existence query
   * @return {@code true} if a matching row is found, {@code false} otherwise
   * @throws MigrationException if the query throws a {@link SQLException}
   */
  @Override
  public Boolean execute(Statement statement) {
    // SELECT * FROM pg_constraint

    try (ResultSet rs =
        statement.executeQuery(
            String.format("select * from dbo.sysobjects where name = '%s'", constraintName))) {
      return rs.next();
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }
}
