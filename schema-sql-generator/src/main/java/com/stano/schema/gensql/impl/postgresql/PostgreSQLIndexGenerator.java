package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.IndexGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreSQLIndexGenerator extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLIndexGenerator.class);

  protected PostgreSQLIndexGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected String getIndexOptions(Key key) {
    if (key.isCompress()) {
      LOGGER.warn("PostgreSQL does not support index compression; ignoring compress on index");
    }

    if (key.getInclude() != null && !key.getInclude().isEmpty()) {
      return String.format("include (%s)", key.getInclude());
    }

    return null;
  }
}
