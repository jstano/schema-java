package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.OtherSql;

/**
 * Represents an {@code other_sql} entry that is new in the new schema, or whose SQL text differs
 * from the old schema's (in which case a {@code DropOtherSqlChange} for the old entry precedes it).
 */
public final class AddOtherSqlChange implements SchemaChange {
  private final OtherSql otherSql;

  /**
   * Creates the change.
   *
   * @param otherSql the entry to run
   */
  public AddOtherSqlChange(OtherSql otherSql) {
    this.otherSql = otherSql;
  }

  /** Returns the entry to run. */
  public OtherSql getOtherSql() {
    return otherSql;
  }
}
