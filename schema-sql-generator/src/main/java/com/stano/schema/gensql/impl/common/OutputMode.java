package com.stano.schema.gensql.impl.common;

/** Controls which parts of a schema a {@link SQLGenerator} outputs. */
public enum OutputMode {
  /**
   * Output the full DDL for the schema: tables, foreign key relations (when {@link
   * com.stano.schema.model.ForeignKeyMode#RELATIONS} is in effect), triggers, functions, views, and
   * procedures.
   */
  ALL,

  /** Output only the DDL that creates indexes. */
  INDEXES_ONLY,

  /** Output only the DDL that creates triggers. */
  TRIGGERS_ONLY
}
