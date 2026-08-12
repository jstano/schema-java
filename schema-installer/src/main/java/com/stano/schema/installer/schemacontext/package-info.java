/**
 * Types describing where a schema (or SQL) definition comes from, and how installation state is
 * tracked, when installing a schema with a {@link com.stano.schema.installer.SchemaInstaller}.
 *
 * <p>{@link com.stano.schema.installer.schemacontext.SchemaContext} is the core interface; {@link
 * com.stano.schema.installer.schemacontext.DefaultSchemaContext} provides a default implementation
 * backed by a fixed URL, and {@link com.stano.schema.installer.schemacontext.FileSchemaContext}
 * extends it to read the schema definition from a local file. {@link
 * com.stano.schema.installer.schemacontext.DataSourceInfo} carries the connection details used to
 * build migration command-line arguments.
 */
package com.stano.schema.installer.schemacontext;
