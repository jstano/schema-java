package com.stano.schema.installer.schemacontext;

/**
 * Connection information for a database data source, used by {@link
 * SchemaContext#getMigrateParams(DataSourceInfo)} to build a command-line migration argument.
 *
 * @param url the JDBC URL of the database
 * @param username the username used to connect to the database
 * @param driverType the JDBC driver type identifier for the database
 */
public record DataSourceInfo(String url, String username, String driverType) {}
