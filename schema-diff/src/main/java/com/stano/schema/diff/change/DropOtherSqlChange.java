package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.OtherSql;

/**
 * Represents an {@code other_sql} entry that no longer exists in the new schema. There is no
 * general way to undo arbitrary SQL, so generators only emit a review comment for it.
 */
public final class DropOtherSqlChange implements SchemaChange {
  private final OtherSql otherSql;

  /**
   * Creates the change.
   *
   * @param otherSql the removed entry
   */
  public DropOtherSqlChange(OtherSql otherSql) {
    this.otherSql = otherSql;
  }

  /** Returns the removed entry. */
  public OtherSql getOtherSql() {
    return otherSql;
  }
}
