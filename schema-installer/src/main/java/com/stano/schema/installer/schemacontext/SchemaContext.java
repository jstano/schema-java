package com.stano.schema.installer.schemacontext;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.ForeignKeyMode;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

public interface SchemaContext {
  URL getSchemaUrl();

  String getMigrationScriptLocator(Connection connection);

  String getPostCreateScriptLocator(Connection connection);

  BooleanMode getBooleanMode();

  ForeignKeyMode getForeignKeyMode();

  boolean schemaIsInstalled(Connection connection) throws SQLException;

  void schemaInstalled(Connection connection) throws SQLException;

  default String getMigrateParams(DataSourceInfo dataSourceInfo) {

    return String.format(
        "--migrate=%s,%s,%s,%s",
        dataSourceInfo.url(), dataSourceInfo.username(), "xxxxxx", dataSourceInfo.driverType());
  }
}
