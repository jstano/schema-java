/**
 * Concrete {@link com.stano.schema.installer.SchemaInstaller} implementation that installs schemas
 * and runs migrations using <a href="https://www.liquibase.org/">Liquibase</a>.
 *
 * <p>{@link com.stano.schema.installer.liquibase.LiquibaseSchemaInstaller} is the entry point;
 * {@link com.stano.schema.installer.liquibase.LiquibaseRuntimeException} wraps failures raised
 * while executing SQL or migration scripts through Liquibase.
 */
package com.stano.schema.installer.liquibase;
