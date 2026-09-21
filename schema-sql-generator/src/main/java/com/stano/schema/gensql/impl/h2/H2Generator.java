package com.stano.schema.gensql.impl.h2;

import com.stano.schema.gensql.impl.common.ColumnConstraintGenerator;
import com.stano.schema.gensql.impl.common.IndexGenerator;
import com.stano.schema.gensql.impl.common.RelationGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.gensql.impl.common.TableGenerator;
import com.stano.schema.gensql.impl.common.ViewGenerator;

public class H2Generator extends SQLGenerator {

  private final TableGenerator tableGenerator;
  private final RelationGenerator relationGenerator;
  private final IndexGenerator indexGenerator;
  private final ViewGenerator viewGenerator;

  public H2Generator(SQLGeneratorOptions sqlGeneratorOptions) {

    super(sqlGeneratorOptions);

    this.tableGenerator = new H2TableGenerator(this);
    this.relationGenerator = new H2RelationGenerator(this);
    this.indexGenerator = new H2IndexGenerator(this);
    this.viewGenerator = new H2ViewGenerator(this);
  }

  @Override
  protected void outputTables() {

    tableGenerator.outputTables();
  }

  @Override
  protected void outputRelations() {

    relationGenerator.outputRelations();
  }

  @Override
  protected void outputIndexes() {

    indexGenerator.outputIndexes();
  }

  @Override
  protected void outputOtherSqlTop() {}

  @Override
  protected void outputOtherSqlBottom() {}

  @Override
  protected void outputTriggers() {}

  @Override
  protected void outputFunctions() {}

  @Override
  protected void outputViews() {

    viewGenerator.outputViews();
  }

  @Override
  protected void outputProcedures() {}

  @Override
  public ColumnConstraintGenerator getColumnConstraintGenerator() {
    return new H2ColumnConstraintGenerator(this);
  }
}
