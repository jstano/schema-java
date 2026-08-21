package com.stano.schema.gensql.impl.postgresql;

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
import com.stano.schema.model.EnumType;
import java.util.Comparator;

public class PostgreSQLGenerator extends SQLGenerator {

  private final TableGenerator tableGenerator;
  private final RelationGenerator relationGenerator;
  private final IndexGenerator indexGenerator;
  private final FunctionGenerator functionGenerator;
  private final ViewGenerator viewGenerator;
  private final ProcedureGenerator procedureGenerator;
  private final TriggerGenerator triggerGenerator;
  private final OtherSqlGenerator otherSqlGenerator;

  public PostgreSQLGenerator(SQLGeneratorOptions sqlGeneratorOptions) {

    super(sqlGeneratorOptions);

    this.tableGenerator = new PostgreSQLTableGenerator(this);
    this.relationGenerator = new PostgreSQLRelationGenerator(this);
    this.indexGenerator = new PostgreSQLIndexGenerator(this);
    this.functionGenerator = new PostgreSQLFunctionGenerator(this);
    this.viewGenerator = new PostgreSQLViewGenerator(this);
    this.procedureGenerator = new PostgreSQLProcedureGenerator(this);
    this.triggerGenerator = new PostgreSQLTriggerGenerator(this);
    this.otherSqlGenerator = new PostgreSQLOtherSqlGenerator(this);
  }

  @Override
  protected void outputHeader() {

    createUUIDGeneratorFunction();
    if (getSqlGeneratorOptions().isEmitPostgresExtensions()) {
      createExtensions();
    }
    createEnumTypes();
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
  protected void outputOtherSqlTop() {

    otherSqlGenerator.outputOtherSqlTop();
  }

  @Override
  protected void outputOtherSqlBottom() {

    otherSqlGenerator.outputOtherSqlBottom();
  }

  private void createUUIDGeneratorFunction() {

    if (getSqlGeneratorOptions().getTargetPostgresVersion() >= 18) {
      return;
    }

    sqlWriter.println(
        "create or replace function generate_uuid() returns uuid language plpgsql volatile"
            + " parallel unsafe as $$");
    sqlWriter.println("declare");
    sqlWriter.println("   -- The current UNIX timestamp in milliseconds");
    sqlWriter.println(
        "   unix_time_ms CONSTANT bigint NOT NULL DEFAULT (extract(epoch FROM clock_timestamp())"
            + " * 1000)::bigint;");
    sqlWriter.println();
    sqlWriter.println(
        "   -- The buffer used to create the UUID: the low 6 bytes (48 bits) of the timestamp,"
            + " followed by 10 random bytes");
    sqlWriter.println(
        "   buffer bytea not null default substring(int8send(unix_time_ms) from 3) ||"
            + " gen_random_bytes(10);");
    sqlWriter.println("begin");
    sqlWriter.println(
        "   -- Set the version nibble of byte 6 to 0111 (UUID v7), keeping the last 4 bits"
            + " unchanged");
    sqlWriter.println("   buffer = set_byte(buffer, 6, (get_byte(buffer, 6) & 15) | 112);");
    sqlWriter.println();
    sqlWriter.println(
        "   -- Set the top 2 bits of byte 8 to 10 (the UUID variant specified in RFC 4122),"
            + " keeping the last 6 bits unchanged");
    sqlWriter.println("   buffer = set_byte(buffer, 8, (get_byte(buffer, 8) & 63) | 128);");
    sqlWriter.println();
    sqlWriter.println("   return encode(buffer, 'hex')::uuid;");
    sqlWriter.println("end");
    sqlWriter.println("$$" + statementSeparator);
    sqlWriter.println();
  }

  private void createExtensions() {

    String checkUser = getSqlGeneratorOptions().getExtensionCheckUser();
    String checkUserExpr = checkUser == null ? "CURRENT_USER" : "'" + checkUser + "'";

    sqlWriter.println("do $$");
    sqlWriter.println("begin");
    sqlWriter.println(
        "   if (select usesuper from pg_user where usename = " + checkUserExpr + ") then");
    sqlWriter.println("      create extension if not exists \"citext\";");
    sqlWriter.println("      create extension if not exists \"btree_gist\";");
    sqlWriter.println("   else");
    sqlWriter.println(
        "      raise notice 'Could not create extensions, user % does not have permission.',"
            + " current_user;");
    sqlWriter.println("   end if;");
    sqlWriter.println("end;");
    sqlWriter.println("$$" + statementSeparator);
    sqlWriter.println();
  }

  private void createEnumTypes() {
    if (schema.getEnumTypes().isEmpty()) {
      return;
    }

    var sortedEnumTypes =
        schema.getEnumTypes().stream()
            .sorted(Comparator.comparing(EnumType::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();

    for (var enumType : sortedEnumTypes) {
      String enumName = enumType.getName().replaceAll("(?<=[a-z0-9])([A-Z])", "_$1").toLowerCase();
      sqlWriter.println("drop type if exists " + enumName + " cascade" + statementSeparator);

      String values =
          enumType.getValues().stream()
              .map(v -> "'" + v.getCode() + "'")
              .collect(java.util.stream.Collectors.joining(","));
      sqlWriter.println(
          "create type " + enumName + " as enum (" + values + ")" + statementSeparator);
      sqlWriter.println();
    }
  }
}
