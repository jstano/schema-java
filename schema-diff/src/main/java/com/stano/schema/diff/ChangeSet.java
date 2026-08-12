package com.stano.schema.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered collection of {@link SchemaChange} instances produced by {@link SchemaDiffEngine} when
 * comparing two {@code Schema} objects. Changes are appended in the order the diff engine discovers
 * them, and that order reflects the sequence in which they should generally be applied (e.g. drops
 * before adds) rather than an arbitrary grouping.
 */
public class ChangeSet {
  private final List<SchemaChange> changes;

  /** Creates a new, empty {@code ChangeSet}. */
  public ChangeSet() {
    this.changes = new ArrayList<>();
  }

  /**
   * Appends a change to the end of this set, preserving discovery order.
   *
   * @param change the change to add
   */
  public void addChange(SchemaChange change) {
    changes.add(change);
  }

  /**
   * Returns the changes in this set, in the order they were added.
   *
   * @return an unmodifiable view of the accumulated changes
   */
  public List<SchemaChange> getChanges() {
    return Collections.unmodifiableList(changes);
  }

  /**
   * Returns whether this set contains no changes, i.e. the two compared schemas were identical.
   *
   * @return {@code true} if no changes have been added, {@code false} otherwise
   */
  public boolean isEmpty() {
    return changes.isEmpty();
  }
}
