package com.stano.schema.reverseengineer.impl.postgresql;

import com.stano.schema.model.Constraint;
import com.stano.schema.model.Schema;
import com.stano.schema.reverseengineer.impl.common.ReverseEngineeringDialect;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQL constraints, and this is the library's own logic moved rather than rewritten: the same
 * catalog query, the same contype letters, the same rules about which of them become a Constraint.
 * It was the only piece of the reader that spoke PostgreSQL, and it is the reason the reader could
 * not run against H2 at all.
 *
 * <p>What the letters mean, and why only two of them are used: PostgreSQL reports p (primary key),
 * u (unique), x (exclusion) and c (check). Primary keys and uniques are read from the JDBC
 * metadata, where they belong, so this step keeps x and c - the two whose text has no other home.
 */
public class PostgreSQLReverseEngineeringDialect implements ReverseEngineeringDialect {

  @Override
  public String productName() {
    return "PostgreSQL";
  }

  @Override
  public void populateConstraints(Schema schema, Connection connection, String schemaName)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT conrelid::regclass AS tablename, conname, contype, pg_get_constraintdef(oid)"
                  + " AS definition"
                  + " FROM"
                  + " pg_constraint;")) {
        while (rs.next()) {
          String tableName = rs.getString("tablename");
          String constraintName = rs.getString("conname");
          String constraintType = rs.getString("contype");
          String definition = rs.getString("definition");

          if (constraintType.equals("p")) {
            // read from the JDBC metadata instead
          } else if (constraintType.equals("u")) {
            // read from the JDBC metadata instead
          } else if (constraintType.equals("x") || constraintType.equals("c")) {
            schema
                .getOptionalTable(tableName)
                .ifPresent(
                    table ->
                        table
                            .getConstraints()
                            .add(new Constraint(constraintName, definition, null)));
          }
        }
      }
    }
  }
}
