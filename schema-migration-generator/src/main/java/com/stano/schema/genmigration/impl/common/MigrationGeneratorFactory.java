package com.stano.schema.genmigration.impl.common;

import com.stano.schema.genmigration.impl.h2.H2MigrationGenerator;
import com.stano.schema.genmigration.impl.postgresql.PostgreSQLMigrationGenerator;
import com.stano.schema.genmigration.impl.sqlserver.SQLServerMigrationGenerator;
import com.stano.schema.model.DatabaseType;

/**
 * Factory that selects and instantiates the dialect-specific {@link MigrationGenerator}
 * implementation matching a given {@link DatabaseType}.
 */
public class MigrationGeneratorFactory {

  /**
   * Creates the {@link MigrationGenerator} implementation matching the database type carried by the
   * given options.
   *
   * @param options the options (including target {@link DatabaseType}) to configure the generator
   *     with
   * @return a {@link PostgreSQLMigrationGenerator}, {@link H2MigrationGenerator}, or {@link
   *     SQLServerMigrationGenerator}, depending on {@link
   *     MigrationGeneratorOptions#getDatabaseType()}
   * @throws IllegalArgumentException if the options' database type is not supported
   */
  public MigrationGenerator createGenerator(MigrationGeneratorOptions options) {
    DatabaseType databaseType = options.getDatabaseType();
    switch (databaseType) {
      case POSTGRESQL:
        return new PostgreSQLMigrationGenerator(options);
      case H2:
        return new H2MigrationGenerator(options);
      case SQL_SERVER:
        return new SQLServerMigrationGenerator(options);
      default:
        throw new IllegalArgumentException("Unsupported database type: " + databaseType);
    }
  }
}
