package com.stano.schema.genmigration;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.SchemaDiffEngine;
import com.stano.schema.genmigration.impl.common.MigrationGenerator;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorFactory;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;

public class GenMigration {
  public MigrationGeneratorFactory migrationGeneratorFactory = new MigrationGeneratorFactory();

  public void generateMigrationSQL(
      Schema oldSchema, Schema newSchema, DatabaseType databaseType, PrintWriter writer) {
    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);
    generateMigrationSQL(databaseType, changeSet, writer);
  }

  public void generateMigrationSQL(
      DatabaseType databaseType,
      ChangeSet changeSet,
      PrintWriter writer,
      String statementSeparator) {
    MigrationGeneratorOptions options =
        new MigrationGeneratorOptions(changeSet, writer, databaseType, statementSeparator);
    MigrationGenerator generator = migrationGeneratorFactory.createGenerator(options);
    generator.generate();
  }

  public void generateMigrationSQL(
      DatabaseType databaseType, ChangeSet changeSet, PrintWriter writer) {
    MigrationGeneratorOptions options =
        new MigrationGeneratorOptions(changeSet, writer, databaseType);
    MigrationGenerator generator = migrationGeneratorFactory.createGenerator(options);
    generator.generate();
  }
}
