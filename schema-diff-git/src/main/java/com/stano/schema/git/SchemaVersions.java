package com.stano.schema.git;

import com.stano.schema.model.Schema;

/**
 * Immutable pair holding the git-committed and current working-tree versions of the same schema XML
 * file, as parsed by {@link GitSchemaReader}.
 */
public class SchemaVersions {
  private final Schema committedSchema;
  private final Schema currentSchema;

  /**
   * Creates a new pair of schema versions.
   *
   * @param committedSchema the schema as parsed from the version committed at {@code HEAD}
   * @param currentSchema the schema as parsed from the current working-tree file
   */
  public SchemaVersions(Schema committedSchema, Schema currentSchema) {
    this.committedSchema = committedSchema;
    this.currentSchema = currentSchema;
  }

  /**
   * Returns the schema as parsed from the version committed at {@code HEAD}.
   *
   * @return the committed schema
   */
  public Schema getCommittedSchema() {
    return committedSchema;
  }

  /**
   * Returns the schema as parsed from the current working-tree file.
   *
   * @return the current schema
   */
  public Schema getCurrentSchema() {
    return currentSchema;
  }
}
