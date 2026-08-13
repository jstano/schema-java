package com.stano.schema.installer.flyway;

import com.stano.schema.installer.schemacontext.DefaultSchemaContext;
import com.stano.schema.installer.schemacontext.SchemaContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Installs a {@code schema.xml} definition into a live database using {@link FlywaySchemaInstaller}
 * — installing it if it isn't present yet, or running any pending migration scripts if it is
 * (mirroring {@code SchemaManager.installOrMigrate}, used at Spring Boot application startup).
 *
 * <p>Connection credentials are read from environment variables rather than command-line arguments,
 * so they don't leak into process listings ({@code ps}) the way positional arguments would.
 *
 * <p>Usage: {@code InstallSchema <schema-filename> [migration-script-locator]}
 *
 * <p>Required environment variables: {@code SCHEMA_JDBC_URL}, {@code SCHEMA_JDBC_USERNAME}, and
 * {@code SCHEMA_JDBC_PASSWORD}.
 */
public class InstallSchema {
  public static void main(String[] args) {
    try {
      if (args.length < 1) {
        System.out.println("USAGE: InstallSchema <schema-filename> [migration-script-locator]");
        System.out.println(
            "   Requires SCHEMA_JDBC_URL, SCHEMA_JDBC_USERNAME, and SCHEMA_JDBC_PASSWORD "
                + "environment variables to be set.");
        System.exit(1);
      }

      String jdbcUrl = requireEnv("SCHEMA_JDBC_URL");
      String username = requireEnv("SCHEMA_JDBC_USERNAME");
      String password = requireEnv("SCHEMA_JDBC_PASSWORD");
      URL schemaUrl = toUrl(new File(args[0]));
      String migrationScriptLocator = args.length > 1 ? args[1] : null;

      new InstallSchema()
          .installOrMigrate(jdbcUrl, username, password, schemaUrl, migrationScriptLocator);

      System.out.println("Schema install/migration completed successfully.");
    } catch (Throwable x) {
      x.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Installs the schema at {@code schemaUrl} if it isn't already installed on the database
   * reachable through {@code jdbcUrl}, otherwise runs any pending migration scripts at {@code
   * migrationScriptLocator}.
   *
   * @param jdbcUrl the JDBC URL of the target database
   * @param username the username used to connect to the database
   * @param password the password used to connect to the database
   * @param schemaUrl the location of the schema definition to install
   * @param migrationScriptLocator the location of migration scripts to run once the schema is
   *     installed, or {@code null} to use {@link FlywaySchemaInstaller}'s default ({@code
   *     db/migration} on the classpath)
   * @throws SQLException if a connection cannot be obtained or installation/migration fails
   */
  void installOrMigrate(
      String jdbcUrl,
      String username,
      String password,
      URL schemaUrl,
      String migrationScriptLocator)
      throws SQLException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
      FlywaySchemaInstaller installer = new FlywaySchemaInstaller();
      SchemaContext schemaContext = new DefaultSchemaContext(schemaUrl, migrationScriptLocator);

      if (schemaContext.schemaIsInstalled(connection)) {
        installer.migrateSchema(connection, schemaContext);
      } else {
        installer.installSchema(connection, schemaContext);
      }
    }
  }

  private static String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Required environment variable " + name + " is not set");
    }
    return value;
  }

  private static URL toUrl(File schemaFile) {
    try {
      return schemaFile.toURI().toURL();
    } catch (MalformedURLException x) {
      throw new IllegalArgumentException(x);
    }
  }
}
