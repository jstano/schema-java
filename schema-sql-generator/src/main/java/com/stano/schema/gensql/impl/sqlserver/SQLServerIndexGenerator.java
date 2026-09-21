package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.IndexGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Key;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.List;

class SQLServerIndexGenerator extends IndexGenerator {
  SQLServerIndexGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected String getFullyQualifiedTableName(Table table) {
    String schemaName =
        table.getSchemaName().equalsIgnoreCase("public") ? "dbo" : table.getSchemaName();
    return schemaName + "." + table.getName();
  }

  @Override
  protected String getIndexOptions(Key key) {
    List<String> options = new ArrayList<>();

    if (key.getInclude() != null && !key.getInclude().isEmpty()) {
      options.add(String.format("include (%s)", key.getInclude()));
    }

    if (key.isCompress()) {
      options.add("with (data_compression = page)");
    }

    return options.isEmpty() ? null : String.join(" ", options);
  }
}
