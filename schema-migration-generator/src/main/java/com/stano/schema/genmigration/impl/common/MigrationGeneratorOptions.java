package com.stano.schema.genmigration.impl.common;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;

public class MigrationGeneratorOptions {
  private final ChangeSet changeSet;
  private final PrintWriter writer;
  private final DatabaseType databaseType;
  private final String statementSeparator;
  private final Schema schema;

  public MigrationGeneratorOptions(
      ChangeSet changeSet,
      PrintWriter writer,
      DatabaseType databaseType,
      String statementSeparator) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = statementSeparator;
    this.schema = null;
  }

  public MigrationGeneratorOptions(
      ChangeSet changeSet, PrintWriter writer, DatabaseType databaseType) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = databaseType.getStatementSeparator();
    this.schema = null;
  }

  public MigrationGeneratorOptions(
      ChangeSet changeSet, PrintWriter writer, DatabaseType databaseType, Schema schema) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = databaseType.getStatementSeparator();
    this.schema = schema;
  }

  public MigrationGeneratorOptions(
      ChangeSet changeSet,
      PrintWriter writer,
      DatabaseType databaseType,
      String statementSeparator,
      Schema schema) {
    this.changeSet = changeSet;
    this.writer = writer;
    this.databaseType = databaseType;
    this.statementSeparator = statementSeparator;
    this.schema = schema;
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

  public Schema getSchema() {
    return schema;
  }
}
