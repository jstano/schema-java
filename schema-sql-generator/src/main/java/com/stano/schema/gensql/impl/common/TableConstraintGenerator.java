package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.Constraint;
import com.stano.schema.model.Table;
import java.util.List;

public class TableConstraintGenerator extends BaseGenerator {
  private static final String CK_PREFIX = "ck_";

  protected TableConstraintGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public List<String> getTableCheckConstraints(Table table) {
    List<Constraint> columns = table.getConstraints();

    return columns.stream()
        .filter(
            constraint ->
                constraint.getDatabaseType() == null
                    || constraint.getDatabaseType() == databaseType)
        .map(constraint -> generateConstraint(table, constraint))
        .toList();
  }

  private String generateConstraint(Table table, Constraint constraint) {
    return String.format("   constraint %s %s", constraint.getName(), constraint.getSql());
  }

  //  private String getConstraintName(String tableName) {
  //    tableName = tableName.toLowerCase();
  //
  //    String name = tableName;
  //    String hashCode = Integer.toHexString(name.hashCode()).toUpperCase();
  //
  //    if (tableName.length() > 18) {
  //      tableName = tableName.substring(0, 18);
  //    }
  //
  //    return CK_PREFIX + tableName + "_" + hashCode;
  //  }
}
