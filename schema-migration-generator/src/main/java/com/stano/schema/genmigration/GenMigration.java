package com.stano.schema.genmigration;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.SchemaDiffEngine;
import com.stano.schema.genmigration.impl.common.MigrationGenerator;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorFactory;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;

/**
 * Entry point for turning schema changes into dialect-specific migration SQL.
 *
 * <p>{@code GenMigration} either accepts a {@link ChangeSet} directly, or derives one from two
 * {@link Schema} instances via {@link SchemaDiffEngine}, then delegates SQL generation to the
 * {@link MigrationGenerator} produced by {@link MigrationGeneratorFactory} for the requested {@link
 * DatabaseType}, writing the resulting statements to the supplied {@link PrintWriter}.
 */
public class GenMigration {
  public MigrationGeneratorFactory migrationGeneratorFactory = new MigrationGeneratorFactory();

  /**
   * Diffs two schema versions and writes the migration SQL needed to evolve {@code oldSchema} into
   * {@code newSchema}, using the target database's default statement separator.
   *
   * @param oldSchema the starting schema version
   * @param newSchema the target schema version
   * @param databaseType the target database dialect
   * @param writer the destination for the generated SQL statements
   */
  public void generateMigrationSQL(
      Schema oldSchema, Schema newSchema, DatabaseType databaseType, PrintWriter writer) {
    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);
    generateMigrationSQL(databaseType, changeSet, writer);
  }

  /**
   * Generates migration SQL for an existing {@link ChangeSet}, using an explicit statement
   * separator.
   *
   * @param databaseType the target database dialect
   * @param changeSet the set of schema changes to generate SQL for
   * @param writer the destination for the generated SQL statements
   * @param statementSeparator the separator appended after each generated SQL statement
   */
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

  /**
   * Generates migration SQL for an existing {@link ChangeSet}, using the target database's default
   * statement separator.
   *
   * @param databaseType the target database dialect
   * @param changeSet the set of schema changes to generate SQL for
   * @param writer the destination for the generated SQL statements
   */
  public void generateMigrationSQL(
      DatabaseType databaseType, ChangeSet changeSet, PrintWriter writer) {
    MigrationGeneratorOptions options =
        new MigrationGeneratorOptions(changeSet, writer, databaseType);
    MigrationGenerator generator = migrationGeneratorFactory.createGenerator(options);
    generator.generate();
  }
}
