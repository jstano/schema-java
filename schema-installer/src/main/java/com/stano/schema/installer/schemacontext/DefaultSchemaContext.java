package com.stano.schema.installer.schemacontext;

import com.stano.schema.migrations.MigrationServices;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.ForeignKeyMode;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base {@link SchemaContext} implementation backed by a fixed schema {@link URL} and an optional
 * fixed migration script locator.
 *
 * <p>By default, {@link #getBooleanMode()} returns {@link BooleanMode#NATIVE}, {@link
 * #getForeignKeyMode()} returns {@link ForeignKeyMode#RELATIONS}, no post-create script is
 * configured, and a schema is considered already installed when a {@code databaseupgradelog} table
 * exists on the target connection. Subclasses such as {@link FileSchemaContext} may override this
 * behavior; {@code FileSchemaContext} in particular always reports the schema as not yet installed.
 */
public class DefaultSchemaContext implements SchemaContext {
  private final URL schemaUrl;
  private final String migrationScriptLocator;

  protected MigrationServices migrationServices = new MigrationServices();

  /**
   * Creates a context for the given schema with no migration script locator configured.
   *
   * @param schemaUrl the location of the schema definition to install
   */
  public DefaultSchemaContext(URL schemaUrl) {
    this(schemaUrl, null);
  }

  /**
   * Creates a context for the given schema and migration script locator.
   *
   * @param schemaUrl the location of the schema definition to install
   * @param migrationScriptLocator the location of migration scripts to run, or {@code null} if none
   */
  public DefaultSchemaContext(URL schemaUrl, String migrationScriptLocator) {
    this.schemaUrl = schemaUrl;
    this.migrationScriptLocator = migrationScriptLocator;
  }

  /**
   * {@inheritDoc}
   *
   * @return the schema URL passed to the constructor
   */
  @Override
  public URL getSchemaUrl() {
    return schemaUrl;
  }

  /**
   * {@inheritDoc}
   *
   * @param connection unused; the locator is fixed at construction time
   * @return the migration script locator passed to the constructor, or {@code null} if none
   */
  @Override
  public String getMigrationScriptLocator(Connection connection) {
    return migrationScriptLocator;
  }

  /**
   * {@inheritDoc}
   *
   * @param connection unused
   * @return always {@code null}; no post-create script is configured by default
   */
  @Override
  public String getPostCreateScriptLocator(Connection connection) {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @return always {@link BooleanMode#NATIVE}
   */
  @Override
  public BooleanMode getBooleanMode() {
    return BooleanMode.NATIVE;
  }

  /**
   * {@inheritDoc}
   *
   * @return always {@link ForeignKeyMode#RELATIONS}
   */
  @Override
  public ForeignKeyMode getForeignKeyMode() {
    return ForeignKeyMode.RELATIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @param connection the connection to the target database
   * @return {@code true} if a {@code databaseupgradelog} table exists on the connection
   * @throws SQLException if the check cannot be performed
   */
  @Override
  public boolean schemaIsInstalled(Connection connection) throws SQLException {
    return migrationServices.tableExists(connection, "databaseupgradelog");
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation does nothing.
   *
   * @param connection the connection to the target database
   * @throws SQLException never thrown by this implementation
   */
  @Override
  public void schemaInstalled(Connection connection) throws SQLException {}
}
