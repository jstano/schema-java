package com.stano.schema.gensql.impl.common;

import com.stano.schema.gensql.impl.h2.H2Generator;
import com.stano.schema.gensql.impl.postgresql.PostgreSQLGenerator;
import com.stano.schema.gensql.impl.sqlserver.SQLServerGenerator;
import com.stano.schema.model.DatabaseType;

public class SQLGeneratorFactory {

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
