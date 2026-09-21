package com.stano.schema.gensql.impl.h2;

import com.stano.schema.gensql.impl.common.ColumnConstraintGenerator;
import com.stano.schema.gensql.impl.common.ColumnGenerator;
import com.stano.schema.gensql.impl.common.IndexGenerator;
import com.stano.schema.gensql.impl.common.KeyGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.TableConstraintGenerator;
import com.stano.schema.gensql.impl.common.TableGenerator;
import com.stano.schema.model.Table;

class H2TableGenerator extends TableGenerator {
  private final ColumnGenerator columnGenerator;
  private final KeyGenerator keyGenerator;
  private final ColumnConstraintGenerator columnConstraintGenerator;
  private final TableConstraintGenerator tableConstraintGenerator;
  private final IndexGenerator indexGenerator;

  H2TableGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);

    this.columnGenerator = new H2ColumnGenerator(sqlGenerator);
    this.keyGenerator = new H2KeyGenerator(sqlGenerator);
    this.columnConstraintGenerator = new H2ColumnConstraintGenerator(sqlGenerator);
    this.tableConstraintGenerator = new H2TableConstraintGenerator(sqlGenerator);
    this.indexGenerator = new H2IndexGenerator(sqlGenerator);
  }

  @Override
  protected ColumnGenerator getColumnGenerator() {
    return columnGenerator;
  }

  @Override
  protected KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  @Override
  protected ColumnConstraintGenerator getColumnConstraintGenerator() {
    return columnConstraintGenerator;
  }

  @Override
  protected TableConstraintGenerator getTableConstraintGenerator() {
    return tableConstraintGenerator;
  }

  @Override
  protected IndexGenerator getIndexGenerator() {
    return indexGenerator;
  }

  @Override
  protected void outputTableDrop(Table table) {
    String tableName = getFullyQualifiedTableName(table);

    sqlWriter.println(String.format("/* %s */", tableName));
    sqlWriter.println("drop table if exists " + tableName + " cascade" + statementSeparator);
  }

  @Override
  protected void outputTableHeader(Table table) {
    String tableName = getFullyQualifiedTableName(table);

    sqlWriter.println("create table " + tableName);
    sqlWriter.println("(");
  }
}
