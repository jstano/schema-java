/**
 * Reads and diffs schema XML definitions across git revisions, and exposes this as a command-line
 * tool.
 *
 * <p>{@link com.stano.schema.git.GitSchemaReader} uses JGit to load the {@code HEAD}-committed and
 * current working-tree versions of a schema XML file into {@link com.stano.schema.model.Schema}
 * instances, paired together in a {@link com.stano.schema.git.SchemaVersions}. {@link
 * com.stano.schema.git.GitSchemaDiffCli} ties this together with {@code schema-diff} and {@code
 * schema-migration-generator} into a single command that diffs a schema file's committed and
 * working-tree versions and writes the resulting migration SQL.
 */
package com.stano.schema.git;
