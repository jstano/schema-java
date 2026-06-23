package com.stano.schema.genmigration.impl.common;

import com.stano.schema.genmigration.impl.h2.H2MigrationGenerator;
import com.stano.schema.genmigration.impl.postgresql.PostgreSQLMigrationGenerator;
import com.stano.schema.genmigration.impl.sqlserver.SQLServerMigrationGenerator;
import com.stano.schema.model.DatabaseType;

public class MigrationGeneratorFactory {

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
