/**
 * Concrete {@link com.stano.schema.installer.SchemaInstaller} implementation that installs schemas
 * and runs migrations using <a href="https://flywaydb.org/">Flyway</a>.
 *
 * <p>{@link com.stano.schema.installer.flyway.FlywaySchemaInstaller} is the entry point; {@link
 * com.stano.schema.installer.flyway.FlywayRuntimeException} wraps failures raised while executing
 * SQL or migration scripts through Flyway.
 */
package com.stano.schema.installer.flyway;
