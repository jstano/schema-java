package com.stano.schema.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChangeSet {
  private final List<SchemaChange> changes;

  public ChangeSet() {
    this.changes = new ArrayList<>();
  }

  public void addChange(SchemaChange change) {
    changes.add(change);
  }

  public List<SchemaChange> getChanges() {
    return Collections.unmodifiableList(changes);
  }

  public boolean isEmpty() {
    return changes.isEmpty();
  }
}
