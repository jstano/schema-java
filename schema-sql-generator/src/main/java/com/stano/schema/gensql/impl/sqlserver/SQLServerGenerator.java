package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.ColumnConstraintGenerator;
import com.stano.schema.gensql.impl.common.FunctionGenerator;
import com.stano.schema.gensql.impl.common.IndexGenerator;
import com.stano.schema.gensql.impl.common.OtherSqlGenerator;
import com.stano.schema.gensql.impl.common.ProcedureGenerator;
import com.stano.schema.gensql.impl.common.RelationGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.gensql.impl.common.TableGenerator;
import com.stano.schema.gensql.impl.common.TriggerGenerator;
import com.stano.schema.gensql.impl.common.ViewGenerator;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SQLServerGenerator extends SQLGenerator {

  private final TableGenerator tableGenerator;
  private final RelationGenerator relationGenerator;
  private final IndexGenerator indexGenerator;
  private final FunctionGenerator functionGenerator;
  private final ViewGenerator viewGenerator;
  private final ProcedureGenerator procedureGenerator;
  private final TriggerGenerator triggerGenerator;
  private final OtherSqlGenerator otherSqlGenerator;

  public SQLServerGenerator(SQLGeneratorOptions sqlGeneratorOptions) {

    super(sqlGeneratorOptions);

    this.tableGenerator = new SQLServerTableGenerator(this);
    this.relationGenerator = new SQLServerRelationGenerator(this);
    this.indexGenerator = new SQLServerIndexGenerator(this);
    this.functionGenerator = new SQLServerFunctionGenerator(this);
    this.viewGenerator = new SQLServerViewGenerator(this);
    this.procedureGenerator = new SQLServerProcedureGenerator(this);
    this.triggerGenerator = new SQLServerTriggerGenerator(this);
    this.otherSqlGenerator = new SQLServerOtherSqlGenerator(this);
  }

  @Override
  protected void outputHeader() {

    createSchemas();
  }

  /**
   * Emits {@code create schema} for every non-default schema referenced by the model. {@code CREATE
   * SCHEMA} must be the first statement in its batch on SQL Server, so this uses dynamic SQL (an
   * {@code exec(...)}) instead. {@code dbo} is skipped - SQL Server always has it.
   */
  private void createSchemas() {

    List<String> schemaNames =
        schema.getTables().stream()
            .map(Table::getSchemaName)
            .map(name -> name.equalsIgnoreCase("public") ? "dbo" : name)
            .filter(name -> !name.equalsIgnoreCase("dbo"))
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)),
                    ArrayList::new));

    if (schemaNames.isEmpty()) {
      return;
    }

    for (String schemaName : schemaNames) {
      sqlWriter.println(
          "if not exists (select 1 from sys.schemas where name = '"
              + schemaName.replace("'", "''")
              + "') exec('create schema "
              + schemaName
              + "')"
              + statementSeparator);
    }
    sqlWriter.println();
  }

  @Override
  protected void outputTables() {

    tableGenerator.outputTables();
  }

  @Override
  protected void outputIndexes() {

    indexGenerator.outputIndexes();
  }

  @Override
  protected void outputRelations() {

    relationGenerator.outputRelations();
  }

  @Override
  protected void outputOtherSqlTop() {

    otherSqlGenerator.outputOtherSqlTop();
  }

  @Override
  protected void outputOtherSqlBottom() {

    otherSqlGenerator.outputOtherSqlBottom();
  }

  @Override
  protected void outputTriggers() {

    triggerGenerator.outputTriggers();
  }

  @Override
  protected void outputFunctions() {

    functionGenerator.outputFunctions();
  }

  @Override
  protected void outputViews() {

    viewGenerator.outputViews();
  }

  @Override
  protected void outputProcedures() {

    procedureGenerator.outputProcedures();
  }

  @Override
  public ColumnConstraintGenerator getColumnConstraintGenerator() {
    return new SQLServerColumnConstraintGenerator(this);
  }
}
