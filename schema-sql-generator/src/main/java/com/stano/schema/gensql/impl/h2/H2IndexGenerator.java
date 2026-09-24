package com.stano.schema.gensql.impl.h2;

import com.stano.schema.gensql.impl.common.IndexGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class H2IndexGenerator extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(H2IndexGenerator.class);

  H2IndexGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  @Override
  protected String getIndexOptions(Key key) {
    if (key.getFilter() != null && !key.getFilter().isEmpty()) {
      LOGGER.warn("H2 does not support partial/filtered indexes; ignoring where on index");
    }

    return super.getIndexOptions(key);
  }
}
