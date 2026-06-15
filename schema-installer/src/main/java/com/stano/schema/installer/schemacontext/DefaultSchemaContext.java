package com.stano.schema.installer.schemacontext;

import com.stano.schema.migrations.MigrationServices;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.ForeignKeyMode;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

public class DefaultSchemaContext implements SchemaContext {
  private final URL schemaUrl;
  private final String migrationScriptLocator;

  protected MigrationServices migrationServices = new MigrationServices();

  public DefaultSchemaContext(URL schemaUrl) {
    this(schemaUrl, null);
  }

  public DefaultSchemaContext(URL schemaUrl, String migrationScriptLocator) {
    this.schemaUrl = schemaUrl;
    this.migrationScriptLocator = migrationScriptLocator;
  }

  @Override
  public URL getSchemaUrl() {
    return schemaUrl;
  }

  @Override
  public String getMigrationScriptLocator(Connection connection) {
    return migrationScriptLocator;
  }

  @Override
  public String getPostCreateScriptLocator(Connection connection) {
    return null;
  }

  @Override
  public BooleanMode getBooleanMode() {
    return BooleanMode.NATIVE;
  }

  @Override
  public ForeignKeyMode getForeignKeyMode() {
    return ForeignKeyMode.RELATIONS;
  }

  @Override
  public boolean schemaIsInstalled(Connection connection) throws SQLException {
    return migrationServices.tableExists(connection, "databaseupgradelog");
  }

  @Override
  public void schemaInstalled(Connection connection) throws SQLException {}
}
