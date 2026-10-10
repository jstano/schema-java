package com.stano.schema.reverseengineer.impl.h2;

import com.stano.schema.model.Constraint;
import com.stano.schema.model.Schema;
import com.stano.schema.reverseengineer.impl.common.ReverseEngineeringDialect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * H2 constraints, read from the information schema - the piece that did not exist, and the reason
 * the reader could not run against H2 at all.
 *
 * <p>What H2 offers, and what it does not: there is no pg_get_constraintdef, so the SQL text of a
 * constraint is available only for those H2 actually stores as a clause - CHECK constraints, in
 * INFORMATION_SCHEMA.CHECK_CONSTRAINTS.CHECK_CLAUSE. Everything else (TABLE_CONSTRAINTS) is
 * reported by name and type, which is exactly what it is: a name and a type, with no body to carry.
 *
 * <p>Which ones become a Constraint, matching the PostgreSQL dialect step for step: PRIMARY KEY and
 * UNIQUE come from the JDBC metadata and are skipped here; EXCLUSION does not exist in H2; CHECK is
 * the one left, and it is the one whose text H2 does keep.
 *
 * <p>Empty is an honest result: a database with no check constraints adds nothing, and this step
 * must never be the reason a whole definition is refused. The schema name is named and compared in
 * the engine's own case - H2 keeps identifiers in UPPER case - and it arrives from the caller
 * rather than being assumed here.
 */
public class H2ReverseEngineeringDialect implements ReverseEngineeringDialect {

  @Override
  public String productName() {
    return "H2";
  }

  @Override
  public void populateConstraints(Schema schema, Connection connection, String schemaName)
      throws SQLException {
    String sql =
        "SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE"
            + " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc"
            + " JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc"
            + "   ON cc.CONSTRAINT_CATALOG = tc.CONSTRAINT_CATALOG"
            + "  AND cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA"
            + "  AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME"
            + " WHERE tc.TABLE_SCHEMA = ?"
            + "   AND tc.CONSTRAINT_TYPE = 'CHECK'"
            + " ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME";

    try (PreparedStatement prepared = connection.prepareStatement(sql)) {
      prepared.setString(1, schemaName);
      try (ResultSet rs = prepared.executeQuery()) {
        while (rs.next()) {
          String tableName = rs.getString("TABLE_NAME");
          String constraintName = rs.getString("CONSTRAINT_NAME");
          String clause = rs.getString("CHECK_CLAUSE");
          schema
              .getOptionalTable(tableName)
              .ifPresent(
                  table ->
                      table.getConstraints().add(new Constraint(constraintName, clause, null)));
        }
      }
    }
  }
}
