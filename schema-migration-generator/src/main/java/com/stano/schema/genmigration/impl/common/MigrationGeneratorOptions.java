package com.stano.schema.genmigration.impl.common;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.model.DatabaseType;
import java.io.PrintWriter;

public class MigrationGeneratorOptions {
  private final ChangeSet changeSet;
  private final PrintWriter writer;
  private final DatabaseType databaseType;
  private final String statementSeparator;

  public MigrationGeneratorOptions(
      ChangeSet changeSet,
      PrintWriter writer,
      DatabaseType databaseType,
      String statementSeparator) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = statementSeparator;
  }

  public MigrationGeneratorOptions(
      ChangeSet changeSet, PrintWriter writer, DatabaseType databaseType) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = databaseType.getStatementSeparator();
  }

  public ChangeSet getChangeSet() {
    return changeSet;
  }

  public PrintWriter getWriter() {
    return writer;
  }

  public DatabaseType getDatabaseType() {
    return databaseType;
  }

  public String getStatementSeparator() {
    return statementSeparator;
  }
}
