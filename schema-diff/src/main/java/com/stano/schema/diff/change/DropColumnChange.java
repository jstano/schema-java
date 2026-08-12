package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import java.util.List;

/**
 * Represents a column that exists in the old schema but not in the new one, and so must be dropped
 * from an existing table. Carries the owning table's name, the dropped column's name, and an
 * optional list of {@link #getRenameCandidates() rename candidates}: names of same-typed columns
 * newly added to the same table that this column may have been renamed to. The candidate list is
 * advisory only — {@link com.stano.schema.diff.SchemaDiffEngine} does not itself infer or emit a
 * {@code RenameColumnChange} from it.
 */
public final class DropColumnChange implements SchemaChange {
  private final String tableName;
  private final String columnName;
  private final List<String> renameCandidates;

  /**
   * Creates a change describing a column to drop, with no rename candidates.
   *
   * @param tableName the name of the table the column is dropped from
   * @param columnName the name of the dropped column
   */
  public DropColumnChange(String tableName, String columnName) {
    this(tableName, columnName, List.of());
  }

  /**
   * Creates a change describing a column to drop, along with possible rename candidates.
   *
   * @param tableName the name of the table the column is dropped from
   * @param columnName the name of the dropped column
   * @param renameCandidates names of newly added, same-typed columns on the same table that this
   *     column may have been renamed to; copied defensively
   */
  public DropColumnChange(String tableName, String columnName, List<String> renameCandidates) {
    this.tableName = tableName;
    this.columnName = columnName;
    this.renameCandidates = List.copyOf(renameCandidates);
  }

  /** Returns the name of the table the column is dropped from. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the name of the dropped column. */
  public String getColumnName() {
    return columnName;
  }

  /**
   * Returns the names of newly added, same-typed columns on the same table that this column may
   * have been renamed to. Empty if no such candidates were found.
   *
   * @return an immutable list of candidate column names, possibly empty
   */
  public List<String> getRenameCandidates() {
    return renameCandidates;
  }
}
