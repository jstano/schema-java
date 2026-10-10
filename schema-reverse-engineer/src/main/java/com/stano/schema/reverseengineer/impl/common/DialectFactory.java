package com.stano.schema.reverseengineer.impl.common;

import com.stano.schema.reverseengineer.impl.h2.H2ReverseEngineeringDialect;
import com.stano.schema.reverseengineer.impl.postgresql.PostgreSQLReverseEngineeringDialect;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * Which dialect answers for which engine, kept as a registry rather than a chain of if/else.
 *
 * <p>The engine is named by the driver, through DatabaseMetaData.getDatabaseProductName(), which is
 * what that call exists for: H2 reports H2, PostgreSQL reports PostgreSQL, and the lookup is a map
 * lookup on the lower-cased name. Adding a third engine means writing one implementation and adding
 * one line here - no existing code changes, and nothing in the reader learns about it.
 *
 * <p>Deliberately not a ServiceLoader: dynamic discovery would buy the ability for another team to
 * ship a dialect in their own jar, and it costs configuration and a mechanism to debug. When that
 * need actually appears, this class can be replaced behind the same interface without touching the
 * reader.
 *
 * <p>A database nobody registered gets an exception, not an empty result. Silently skipping a step
 * because the engine was not recognised would produce a definition that looks complete and quietly
 * is not.
 */
public final class DialectFactory {

  private static final Map<String, ReverseEngineeringDialect> DIALECTS =
      Map.of(
          "h2", new H2ReverseEngineeringDialect(),
          "postgresql", new PostgreSQLReverseEngineeringDialect());

  /**
   * The dialect for this connection's engine.
   *
   * @throws IllegalArgumentException when the driver's product name is not registered, naming the
   *     database so the caller can see what was asked for rather than guessing why nothing happened
   */
  public ReverseEngineeringDialect resolve(Connection connection) throws SQLException {
    String productName = connection.getMetaData().getDatabaseProductName();
    String key = productName == null ? "" : productName.trim().toLowerCase(Locale.ROOT);
    ReverseEngineeringDialect dialect = DIALECTS.get(key);
    if (dialect == null) {
      throw new IllegalArgumentException(
          "Unsupported database for reverse engineering: "
              + productName
              + " (registered: "
              + DIALECTS.keySet()
              + ")");
    }
    return dialect;
  }
}
