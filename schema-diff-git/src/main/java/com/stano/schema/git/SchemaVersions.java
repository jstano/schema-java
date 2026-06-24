package com.stano.schema.git;

import com.stano.schema.model.Schema;

public class SchemaVersions {
  private final Schema committedSchema;
  private final Schema currentSchema;

  public SchemaVersions(Schema committedSchema, Schema currentSchema) {
    this.committedSchema = committedSchema;
    this.currentSchema = currentSchema;
  }

  public Schema getCommittedSchema() {
    return committedSchema;
  }

  public Schema getCurrentSchema() {
    return currentSchema;
  }
}
