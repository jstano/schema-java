package com.stano.schema.installer.schemacontext;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.ForeignKeyMode;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Describes where a schema definition comes from and how a {@link
 * com.stano.schema.installer.SchemaInstaller} should install and track it against a particular
 * database.
 *
 * <p>Implementations supply the location of the schema (or raw SQL) to install, optional locations
 * for migration and post-create scripts, the {@link BooleanMode} and {@link ForeignKeyMode} to use
 * when generating SQL, and the logic used to detect and record whether the schema has already been
 * installed on a given connection.
 *
 * @see com.stano.schema.installer.schemacontext.DefaultSchemaContext
 * @see com.stano.schema.installer.schemacontext.FileSchemaContext
 */
public interface SchemaContext {
  /**
   * Returns the location of the schema definition (an XML schema file, or a raw SQL file when used
   * with {@code SchemaInstaller.installSql}) to install.
   *
   * @return the URL of the schema definition
   */
  URL getSchemaUrl();

  /**
   * Returns the location of the migration scripts to run against the given connection, or {@code
   * null} if none is configured for this context.
   *
   * @param connection the connection to the target database
   * @return the migration script locator, or {@code null} if none is configured
   */
  String getMigrationScriptLocator(Connection connection);

  /**
   * Returns the location of a script to run immediately after the schema has been installed on the
   * given connection, or {@code null} if no post-create script is configured.
   *
   * @param connection the connection to the target database
   * @return the post-create script locator, or {@code null} if none is configured
   */
  String getPostCreateScriptLocator(Connection connection);

  /**
   * Returns the boolean representation mode to use when generating SQL for this schema.
   *
   * @return the boolean mode
   */
  BooleanMode getBooleanMode();

  /**
   * Returns the foreign key generation mode to use when generating SQL for this schema.
   *
   * @return the foreign key mode
   */
  ForeignKeyMode getForeignKeyMode();

  /**
   * Determines whether the schema has already been installed on the database reachable through the
   * given connection.
   *
   * @param connection the connection to the target database
   * @return {@code true} if the schema is already installed, {@code false} otherwise
   * @throws SQLException if the check cannot be performed
   */
  boolean schemaIsInstalled(Connection connection) throws SQLException;

  /**
   * Records that the schema has just been installed on the database reachable through the given
   * connection.
   *
   * @param connection the connection to the target database
   * @throws SQLException if the installation cannot be recorded
   */
  void schemaInstalled(Connection connection) throws SQLException;

  /**
   * Builds the {@code --migrate} command-line argument value for the given data source, with the
   * password redacted.
   *
   * @param dataSourceInfo the connection information to encode
   * @return the formatted {@code --migrate=<url>,<username>,xxxxxx,<driverType>} string
   */
  default String getMigrateParams(DataSourceInfo dataSourceInfo) {

    return String.format(
        "--migrate=%s,%s,%s,%s",
        dataSourceInfo.url(), dataSourceInfo.username(), "xxxxxx", dataSourceInfo.driverType());
  }
}
