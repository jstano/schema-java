package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.View;
import java.io.PrintWriter;

public class BaseGenerator {

  protected final SQLGenerator sqlGenerator;
  protected final DatabaseType databaseType;
  protected final PrintWriter sqlWriter;
  protected final Schema schema;
  protected final String statementSeparator;
  protected final ForeignKeyMode foreignKeyMode;
  protected final BooleanMode booleanMode;
  protected final int maxKeyNameLength;

  protected BaseGenerator(SQLGenerator sqlGenerator) {

    this.sqlGenerator = sqlGenerator;
    this.databaseType = sqlGenerator.getSqlGeneratorOptions().getDatabaseType();
    this.sqlWriter = sqlGenerator.getSqlGeneratorOptions().getSqlWriter();
    this.schema = sqlGenerator.getSqlGeneratorOptions().getSchema();
    this.statementSeparator = sqlGenerator.getSqlGeneratorOptions().getStatementSeparator();
    this.foreignKeyMode = sqlGenerator.getSqlGeneratorOptions().getForeignKeyMode();
    this.booleanMode = sqlGenerator.getSqlGeneratorOptions().getBooleanMode();
    this.maxKeyNameLength = this.databaseType.getMaxKeyNameLength();
  }

  protected String getFullyQualifiedTableName(Table table) {

    return table.getSchemaName() + "." + table.getName();
  }

  protected String getFullyQualifiedViewName(View view) {

    return view.getSchemaName() + "." + view.getName();
  }

  protected String buildKeyName(String prefix, String tableName, String suffix) {
    String candidate = prefix + tableName + suffix;

    if (candidate.length() <= maxKeyNameLength) {
      return candidate;
    }

    int available = maxKeyNameLength - (prefix.length() + suffix.length());
    int truncateTo = Math.max(0, Math.min(available, tableName.length()));

    return prefix + tableName.substring(0, truncateTo) + suffix;
  }
}
