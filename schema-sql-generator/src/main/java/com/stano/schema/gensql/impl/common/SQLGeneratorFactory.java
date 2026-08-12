package com.stano.schema.gensql.impl.common;

import com.stano.schema.gensql.impl.h2.H2Generator;
import com.stano.schema.gensql.impl.postgresql.PostgreSQLGenerator;
import com.stano.schema.gensql.impl.sqlserver.SQLServerGenerator;
import com.stano.schema.model.DatabaseType;

/**
 * Factory that returns the appropriate {@link SQLGenerator} implementation for a given {@link
 * DatabaseType}.
 */
public class SQLGeneratorFactory {

  /**
   * Creates a new {@link SQLGenerator} for the database type specified in the given options.
   *
   * @param sqlGeneratorOptions the options for the generator, including the target {@link
   *     DatabaseType}
   * @return a new {@link SQLGenerator} instance appropriate for the requested database type
   * @throws IllegalArgumentException if the options' database type is {@code null} or not one of
   *     the supported types
   */
  public SQLGenerator createSQLGenerator(SQLGeneratorOptions sqlGeneratorOptions) {

    DatabaseType databaseType = sqlGeneratorOptions.getDatabaseType();

    if (databaseType != null) {
      switch (databaseType) {
        case H2:
          return new H2Generator(sqlGeneratorOptions);
        case POSTGRESQL:
          return new PostgreSQLGenerator(sqlGeneratorOptions);
        case SQL_SERVER:
          return new SQLServerGenerator(sqlGeneratorOptions);
      }
    }

    throw new IllegalArgumentException("Unable to locate a generator");
  }
}
