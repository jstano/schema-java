package com.stano.schema.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.AggregationFrequency;
import com.stano.schema.model.AggregationType;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.LockEscalation;
import com.stano.schema.model.OtherSqlOrder;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Table;
import com.stano.schema.model.TableOption;
import com.stano.schema.model.TriggerType;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaParser")
class SchemaParserTest {

  @Test
  @DisplayName("should be able to parse a valid schema file")
  void shouldBeAbleToParseAValidSchemaFile() {
    SchemaParser schemaParser = new SchemaParser();

    var schema =
        schemaParser.parseSchema(
            getClass().getClassLoader().getResource("schema-parser-test-schema.xml"));

    Table parentTable = schema.getTable("ParentTable");
    Table childTable = schema.getTable("ChildTable");
    Table columnTesterTable = schema.getTable("ColumnTesterTable");
    Table kbiTable = schema.getTable("KBI");
    Table unitTable = schema.getTable("Unit");

    assertEquals(schema.getTables().size(), 8);

    verifyParentTable(parentTable);
    verifyChildTable(childTable);
    verifyColumnTesterTable(columnTesterTable);
    verifyKBITable(kbiTable);

    assertEquals(schema.getViews().size(), 4);
    assertEquals(schema.getViews().get(0).getSchemaName(), "test");
    assertEquals(schema.getViews().get(0).getName(), "TestView1");
    assertEquals(schema.getViews().get(0).getSql(), "select * from ParentTable");
    assertFalse(schema.getViews().get(0).getDatabaseType() != null);

    assertEquals(schema.getViews().get(1).getSchemaName(), "public");
    assertEquals(schema.getViews().get(1).getName(), "TestView1");
    assertEquals(schema.getViews().get(1).getSql(), "select * from ParentTable");
    assertFalse(schema.getViews().get(1).getDatabaseType() != null);

    assertEquals(schema.getViews().get(2).getSchemaName(), "public");
    assertEquals(schema.getViews().get(2).getName(), "TestView2");
    assertEquals(schema.getViews().get(2).getSql(), "select * from pgsql");
    assertEquals(schema.getViews().get(2).getDatabaseType(), DatabaseType.POSTGRESQL);

    assertEquals(schema.getViews().get(3).getSchemaName(), "public");
    assertEquals(schema.getViews().get(3).getName(), "TestView2");
    assertEquals(schema.getViews().get(3).getSql(), "select * from mssql");
    assertEquals(schema.getViews().get(3).getDatabaseType(), DatabaseType.SQL_SERVER);

    assertEquals(unitTable.getSchemaName(), "test");

    assertEquals(schema.getEnumTypes().size(), 2);
    assertEquals(schema.getEnumType("GenderType").getName(), "GenderType");
    assertEquals(schema.getEnumType("GenderType").getValues().size(), 2);
    assertEquals(schema.getEnumType("GenderType").getValues().get(0).getName(), "MALE");
    assertEquals(schema.getEnumType("GenderType").getValues().get(0).getCode(), "M");
    assertEquals(schema.getEnumType("GenderType").getValues().get(1).getName(), "FEMALE");
    assertEquals(schema.getEnumType("GenderType").getValues().get(1).getCode(), "F");
    assertEquals(schema.getEnumType("TestEnumType").getName(), "TestEnumType");
    assertEquals(schema.getEnumType("TestEnumType").getValues().size(), 2);
    assertEquals(schema.getEnumType("TestEnumType").getValues().get(0).getName(), "ONE");
    assertEquals(schema.getEnumType("TestEnumType").getValues().get(0).getCode(), "1");
    assertEquals(schema.getEnumType("TestEnumType").getValues().get(1).getName(), "TWO");
    assertEquals(schema.getEnumType("TestEnumType").getValues().get(1).getCode(), "2");

    assertEquals(schema.getOtherSql().size(), 8);
    assertEquals(schema.getOtherSql().get(0).getDatabaseType(), DatabaseType.POSTGRESQL);
    assertEquals(schema.getOtherSql().get(0).getOrder(), OtherSqlOrder.TOP);
    assertEquals(schema.getOtherSql().get(0).getSql(), "other top sql for pgsql 1");
    assertEquals(schema.getOtherSql().get(1).getDatabaseType(), DatabaseType.POSTGRESQL);
    assertEquals(schema.getOtherSql().get(1).getOrder(), OtherSqlOrder.TOP);
    assertEquals(schema.getOtherSql().get(1).getSql(), "other top sql for pgsql 2");
    assertEquals(schema.getOtherSql().get(2).getDatabaseType(), DatabaseType.POSTGRESQL);
    assertEquals(schema.getOtherSql().get(2).getOrder(), OtherSqlOrder.BOTTOM);
    assertEquals(schema.getOtherSql().get(2).getSql(), "other bottom sql for pgsql 1");
    assertEquals(schema.getOtherSql().get(3).getDatabaseType(), DatabaseType.POSTGRESQL);
    assertEquals(schema.getOtherSql().get(3).getOrder(), OtherSqlOrder.BOTTOM);
    assertEquals(schema.getOtherSql().get(3).getSql(), "other bottom sql for pgsql 2");
    assertEquals(schema.getOtherSql().get(4).getDatabaseType(), DatabaseType.SQL_SERVER);
    assertEquals(schema.getOtherSql().get(4).getOrder(), OtherSqlOrder.TOP);
    assertEquals(schema.getOtherSql().get(4).getSql(), "other top sql for mssql 1");
    assertEquals(schema.getOtherSql().get(5).getDatabaseType(), DatabaseType.SQL_SERVER);
    assertEquals(schema.getOtherSql().get(5).getOrder(), OtherSqlOrder.TOP);
    assertEquals(schema.getOtherSql().get(5).getSql(), "other top sql for mssql 2");
    assertEquals(schema.getOtherSql().get(6).getDatabaseType(), DatabaseType.SQL_SERVER);
    assertEquals(schema.getOtherSql().get(6).getOrder(), OtherSqlOrder.BOTTOM);
    assertEquals(schema.getOtherSql().get(6).getSql(), "other bottom sql for mssql 1");
    assertEquals(schema.getOtherSql().get(7).getDatabaseType(), DatabaseType.SQL_SERVER);
    assertEquals(schema.getOtherSql().get(7).getOrder(), OtherSqlOrder.BOTTOM);
    assertEquals(schema.getOtherSql().get(7).getSql(), "other bottom sql for mssql 2");

    assertEquals(schema.getFunctions().size(), 6);
    assertEquals(schema.getFunctions().get(0).getSchemaName(), "test");
    assertEquals(schema.getFunctions().get(0).getName(), "testCustomFunction1");
    assertEquals(schema.getFunctions().get(0).getSql(), "test custom function sql for pgsql 1");
    assertEquals(schema.getFunctions().get(1).getSchemaName(), "test");
    assertEquals(schema.getFunctions().get(1).getName(), "testCustomFunction1");
    assertEquals(schema.getFunctions().get(1).getSql(), "test custom function sql for mssql 1");
    assertEquals(schema.getFunctions().get(2).getSchemaName(), "public");
    assertEquals(schema.getFunctions().get(2).getName(), "customFunction1");
    assertEquals(schema.getFunctions().get(2).getSql(), "custom function sql for pgsql 1");
    assertEquals(schema.getFunctions().get(3).getSchemaName(), "public");
    assertEquals(schema.getFunctions().get(3).getName(), "customFunction1");
    assertEquals(schema.getFunctions().get(3).getSql(), "custom function sql for mssql 1");
    assertEquals(schema.getFunctions().get(4).getSchemaName(), "public");
    assertEquals(schema.getFunctions().get(4).getName(), "customFunction2");
    assertEquals(schema.getFunctions().get(4).getSql(), "custom function sql for pgsql 2");
    assertEquals(schema.getFunctions().get(5).getSchemaName(), "public");
    assertEquals(schema.getFunctions().get(5).getName(), "customFunction2");
    assertEquals(schema.getFunctions().get(5).getSql(), "custom function sql for mssql 2");

    assertEquals(schema.getProcedures().size(), 6);
    assertEquals(schema.getProcedures().get(0).getSchemaName(), "test");
    assertEquals(schema.getProcedures().get(0).getName(), "testCustomProcedure1");
    assertEquals(schema.getProcedures().get(0).getSql(), "test custom procedure sql for pgsql 1");
    assertEquals(schema.getProcedures().get(1).getSchemaName(), "test");
    assertEquals(schema.getProcedures().get(1).getName(), "testCustomProcedure1");
    assertEquals(schema.getProcedures().get(1).getSql(), "test custom procedure sql for mssql 1");
    assertEquals(schema.getProcedures().get(2).getSchemaName(), "public");
    assertEquals(schema.getProcedures().get(2).getName(), "customProcedure1");
    assertEquals(schema.getProcedures().get(2).getSql(), "custom procedure sql for pgsql 1");
    assertEquals(schema.getProcedures().get(3).getSchemaName(), "public");
    assertEquals(schema.getProcedures().get(3).getName(), "customProcedure1");
    assertEquals(schema.getProcedures().get(3).getSql(), "custom procedure sql for mssql 1");
    assertEquals(schema.getProcedures().get(4).getSchemaName(), "public");
    assertEquals(schema.getProcedures().get(4).getName(), "customProcedure2");
    assertEquals(schema.getProcedures().get(4).getSql(), "custom procedure sql for pgsql 2");
    assertEquals(schema.getProcedures().get(5).getSchemaName(), "public");
    assertEquals(schema.getProcedures().get(5).getName(), "customProcedure2");
    assertEquals(schema.getProcedures().get(5).getSql(), "custom procedure sql for mssql 2");
  }

  @Test
  @DisplayName("should get a RuntimeIOException if an IOException occurs")
  void shouldGetARuntimeIOExceptionIfAnIOExceptionOccurs() {
    SchemaParser schemaParser = new SchemaParser();

    assertThrows(
        SchemaParserException.class,
        () -> schemaParser.parseSchema(new URI("file:bad-url").toURL()));
  }

  private void verifyParentTable(Table parentTable) {
    assertEquals(parentTable.getColumns().size(), 4);
    assertEquals(parentTable.getColumns().get(0).getName(), "ID");
    assertEquals(parentTable.getColumns().get(0).getType(), ColumnType.SEQUENCE);
    assertTrue(parentTable.getColumns().get(0).isRequired());
    assertEquals(parentTable.getColumns().get(1).getName(), "Name");
    assertEquals(parentTable.getColumns().get(1).getType(), ColumnType.VARCHAR);
    assertEquals(parentTable.getColumns().get(1).getLength(), 100);
    assertTrue(parentTable.getColumns().get(1).isRequired());
    assertEquals(parentTable.getColumns().get(2).getName(), "Extra");
    assertEquals(parentTable.getColumns().get(2).getType(), ColumnType.VARCHAR);
    assertEquals(parentTable.getColumns().get(2).getLength(), 200);
    assertFalse(parentTable.getColumns().get(2).isRequired());
    assertEquals(parentTable.getColumns().get(3).getName(), "Gender");
    assertEquals(parentTable.getColumns().get(3).getType(), ColumnType.ENUM);
    assertEquals(parentTable.getColumns().get(3).getEnumType(), "GenderType");
    assertFalse(parentTable.getColumns().get(3).isRequired());

    assertEquals(parentTable.getKeys().size(), 2);
    assertEquals(parentTable.getKeys().get(0).getType(), KeyType.PRIMARY);
    assertFalse(parentTable.getKeys().get(0).isCluster());
    assertFalse(parentTable.getKeys().get(0).isCompress());
    assertEquals(parentTable.getKeys().get(0).getColumns().size(), 1);
    assertEquals(parentTable.getKeys().get(0).getColumns().get(0).getName(), "ID");
    assertEquals(parentTable.getKeys().get(1).getType(), KeyType.UNIQUE);
    assertTrue(parentTable.getKeys().get(1).isCluster());
    assertFalse(parentTable.getKeys().get(1).isCompress());
    assertEquals(parentTable.getKeys().get(1).getColumns().size(), 2);
    assertEquals(parentTable.getKeys().get(1).getColumns().get(0).getName(), "Name");
    assertEquals(parentTable.getKeys().get(1).getColumns().get(1).getName(), "Extra");
    assertEquals(parentTable.getIndexes().size(), 2);
    assertEquals(parentTable.getIndexes().get(0).getType(), KeyType.INDEX);
    assertFalse(parentTable.getIndexes().get(0).isCluster());
    assertTrue(parentTable.getIndexes().get(0).isCompress());
    assertEquals(parentTable.getIndexes().get(0).getColumns().size(), 2);
    assertEquals(parentTable.getIndexes().get(0).getColumns().get(0).getName(), "Extra");
    assertEquals(parentTable.getIndexes().get(0).getColumns().get(1).getName(), "Name");
    assertEquals(parentTable.getIndexes().get(1).getType(), KeyType.INDEX);
    assertEquals(parentTable.getIndexes().get(1).getColumns().size(), 3);
    assertEquals(parentTable.getIndexes().get(1).getColumns().get(0).getName(), "ID");
    assertEquals(parentTable.getIndexes().get(1).getColumns().get(1).getName(), "Name");
    assertEquals(parentTable.getIndexes().get(1).getColumns().get(2).getName(), "Extra");

    assertTrue(parentTable.getRelations().isEmpty());

    assertEquals(parentTable.getInitialData().size(), 4);
    assertEquals(
        parentTable.getInitialData().get(0).getSql(),
        "insert into ParentTable (Name,Extra,Gender) values ('AAA','Extra AAA','M')");
    assertEquals(
        parentTable.getInitialData().get(1).getSql(),
        "insert into ParentTable (Name,Extra,Gender) values ('BBB','Extra BBB','F')");
    assertEquals(
        parentTable.getInitialData().get(2).getSql(),
        "insert into ParentTable (Name,Extra,Gender) values ('PGSQL','Extra PGSQL','M')");
    assertEquals(
        parentTable.getInitialData().get(3).getSql(),
        "insert into ParentTable (Name,Extra,Gender) values ('MSSQL','Extra MSSQL','F')");

    assertEquals(parentTable.getTriggers().size(), 4);
    assertEquals(parentTable.getTriggers().get(0).getDatabaseType(), DatabaseType.POSTGRESQL);
    assertEquals(parentTable.getTriggers().get(0).getTriggerType(), TriggerType.DELETE);
    assertEquals(parentTable.getTriggers().get(0).getTriggerText(), "delete from pgsql");
    assertEquals(parentTable.getTriggers().get(1).getDatabaseType(), DatabaseType.SQL_SERVER);
    assertEquals(parentTable.getTriggers().get(1).getTriggerType(), TriggerType.DELETE);
    assertEquals(parentTable.getTriggers().get(1).getTriggerText(), "delete from mssql");
    assertEquals(parentTable.getTriggers().get(2).getDatabaseType(), DatabaseType.POSTGRESQL);
    assertEquals(parentTable.getTriggers().get(2).getTriggerType(), TriggerType.UPDATE);
    assertEquals(parentTable.getTriggers().get(2).getTriggerText(), "update pgsql");
    assertEquals(parentTable.getTriggers().get(3).getDatabaseType(), DatabaseType.SQL_SERVER);
    assertEquals(parentTable.getTriggers().get(3).getTriggerType(), TriggerType.UPDATE);
    assertEquals(parentTable.getTriggers().get(3).getTriggerText(), "update mssql");

    assertEquals(parentTable.getAggregations().size(), 2);
    assertEquals(
        parentTable.getAggregations().get(0).getDestinationTable(), "ParentTableAggregation");
    assertEquals(parentTable.getAggregations().get(0).getDateColumn(), "Extra");
    assertEquals(parentTable.getAggregations().get(0).getCriteria(), "criteria");
    assertEquals(parentTable.getAggregations().get(0).getTimeStampColumn(), "timestamp");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationFrequency(), AggregationFrequency.DAILY);
    assertEquals(parentTable.getAggregations().get(0).getAggregationColumns().size(), 2);
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationColumns().get(0).getAggregationType(),
        AggregationType.COUNT);
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationColumns().get(0).getSourceColumn(),
        null);
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationColumns().get(0).getDestinationColumn(),
        "CountOfData");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationColumns().get(1).getAggregationType(),
        AggregationType.SUM);
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationColumns().get(1).getSourceColumn(),
        "Name");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationColumns().get(1).getDestinationColumn(),
        "SumOfData");
    assertEquals(parentTable.getAggregations().get(0).getAggregationGroups().size(), 2);
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationGroups().get(0).getSource(), "Source1");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationGroups().get(0).getDestination(),
        "Destination1");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationGroups().get(0).getSourceDerivedFrom(),
        "SourceDerivedFrom1");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationGroups().get(1).getSource(), "Source2");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationGroups().get(1).getDestination(),
        "Destination2");
    assertEquals(
        parentTable.getAggregations().get(0).getAggregationGroups().get(1).getSourceDerivedFrom(),
        "SourceDerivedFrom2");

    assertEquals(
        parentTable.getAggregations().get(1).getDestinationTable(), "ParentTableAggregation2");
    assertEquals(parentTable.getAggregations().get(1).getDateColumn(), "Extra");
    assertFalse(parentTable.getAggregations().get(1).getCriteria() != null);
    assertEquals(parentTable.getAggregations().get(1).getTimeStampColumn(), "timestamp");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationFrequency(), AggregationFrequency.DAILY);
    assertEquals(parentTable.getAggregations().get(1).getAggregationColumns().size(), 2);
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationColumns().get(0).getAggregationType(),
        AggregationType.COUNT);
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationColumns().get(0).getSourceColumn(),
        null);
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationColumns().get(0).getDestinationColumn(),
        "CountOfData");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationColumns().get(1).getAggregationType(),
        AggregationType.SUM);
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationColumns().get(1).getSourceColumn(),
        "Name");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationColumns().get(1).getDestinationColumn(),
        "SumOfData");
    assertEquals(parentTable.getAggregations().get(1).getAggregationGroups().size(), 2);
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationGroups().get(0).getSource(), "Source1");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationGroups().get(0).getDestination(),
        "Destination1");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationGroups().get(0).getSourceDerivedFrom(),
        "SourceDerivedFrom1");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationGroups().get(1).getSource(), "Source2");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationGroups().get(1).getDestination(),
        "Destination2");
    assertEquals(
        parentTable.getAggregations().get(1).getAggregationGroups().get(1).getSourceDerivedFrom(),
        "SourceDerivedFrom2");
  }

  private void verifyChildTable(Table childTable) {
    assertEquals(childTable.getRelations().size(), 1);
    assertEquals(childTable.getRelations().get(0).getFromTableName(), "ChildTable");
    assertEquals(childTable.getRelations().get(0).getFromColumnName(), "ParentID");
    assertEquals(childTable.getRelations().get(0).getToTableName(), "ParentTable");
    assertEquals(childTable.getRelations().get(0).getToColumnName(), "ID");
    assertEquals(childTable.getRelations().get(0).getType(), RelationType.CASCADE);
  }

  private void verifyColumnTesterTable(Table columnTesterTable) {
    assertEquals(columnTesterTable.getName(), "ColumnTesterTable");
    assertEquals(columnTesterTable.getOptions().size(), 3);
    assertEquals(columnTesterTable.getOptions().get(0), TableOption.NO_EXPORT);
    assertEquals(columnTesterTable.getOptions().get(1), TableOption.COMPRESS);
    assertEquals(columnTesterTable.getOptions().get(2), TableOption.DATA);
    assertEquals(columnTesterTable.getColumns().size(), 25);
    assertEquals(columnTesterTable.getColumns().get(0).getName(), "sequence");
    assertEquals(columnTesterTable.getColumns().get(0).getType(), ColumnType.SEQUENCE);
    assertEquals(columnTesterTable.getColumns().get(1).getName(), "longsequence");
    assertEquals(columnTesterTable.getColumns().get(1).getType(), ColumnType.LONGSEQUENCE);
    assertEquals(columnTesterTable.getColumns().get(2).getName(), "byte");
    assertEquals(columnTesterTable.getColumns().get(2).getType(), ColumnType.BYTE);
    assertEquals(columnTesterTable.getColumns().get(3).getName(), "short");
    assertEquals(columnTesterTable.getColumns().get(3).getType(), ColumnType.SHORT);
    assertEquals(columnTesterTable.getColumns().get(4).getName(), "int");
    assertEquals(columnTesterTable.getColumns().get(4).getType(), ColumnType.INT);
    assertEquals(columnTesterTable.getColumns().get(4).getMinValue(), "1");
    assertEquals(columnTesterTable.getColumns().get(4).getMaxValue(), "500");
    assertEquals(columnTesterTable.getColumns().get(5).getName(), "long");
    assertEquals(columnTesterTable.getColumns().get(5).getType(), ColumnType.LONG);
    assertEquals(columnTesterTable.getColumns().get(6).getName(), "float");
    assertEquals(columnTesterTable.getColumns().get(6).getType(), ColumnType.FLOAT);
    assertEquals(columnTesterTable.getColumns().get(7).getName(), "double");
    assertEquals(columnTesterTable.getColumns().get(7).getType(), ColumnType.DOUBLE);
    assertEquals(columnTesterTable.getColumns().get(8).getName(), "decimal");
    assertEquals(columnTesterTable.getColumns().get(8).getType(), ColumnType.DECIMAL);
    assertEquals(columnTesterTable.getColumns().get(8).getLength(), 19);
    assertEquals(columnTesterTable.getColumns().get(8).getScale(), 4);
    assertEquals(columnTesterTable.getColumns().get(9).getName(), "boolean");
    assertEquals(columnTesterTable.getColumns().get(9).getType(), ColumnType.BOOLEAN);
    assertEquals(columnTesterTable.getColumns().get(10).getName(), "date");
    assertEquals(columnTesterTable.getColumns().get(10).getType(), ColumnType.DATE);
    assertEquals(columnTesterTable.getColumns().get(11).getName(), "datetime");
    assertEquals(columnTesterTable.getColumns().get(11).getType(), ColumnType.DATETIME);
    assertEquals(columnTesterTable.getColumns().get(12).getName(), "time");
    assertEquals(columnTesterTable.getColumns().get(12).getType(), ColumnType.TIME);
    assertEquals(columnTesterTable.getColumns().get(13).getName(), "timestamp");
    assertEquals(columnTesterTable.getColumns().get(13).getType(), ColumnType.TIMESTAMP);
    assertEquals(columnTesterTable.getColumns().get(14).getName(), "timestamptz");
    assertEquals(columnTesterTable.getColumns().get(14).getType(), ColumnType.TIMESTAMPTZ);
    assertEquals(columnTesterTable.getColumns().get(15).getName(), "char");
    assertEquals(columnTesterTable.getColumns().get(15).getType(), ColumnType.CHAR);
    assertEquals(columnTesterTable.getColumns().get(15).getLength(), 1);
    assertEquals(columnTesterTable.getColumns().get(15).getDefaultConstraint(), "default 'A'");
    assertEquals(columnTesterTable.getColumns().get(16).getName(), "varchar");
    assertEquals(columnTesterTable.getColumns().get(16).getType(), ColumnType.VARCHAR);
    assertEquals(columnTesterTable.getColumns().get(16).getLength(), 10);
    assertEquals(columnTesterTable.getColumns().get(17).getName(), "varcharWithCheck");
    assertEquals(columnTesterTable.getColumns().get(17).getType(), ColumnType.VARCHAR);
    assertEquals(columnTesterTable.getColumns().get(17).getLength(), 6);
    assertEquals(
        columnTesterTable.getColumns().get(17).getCheckConstraint(),
        "check(varcharWithCheck = 'ABC123')");
    assertEquals(columnTesterTable.getColumns().get(18).getName(), "enum");
    assertEquals(columnTesterTable.getColumns().get(18).getType(), ColumnType.ENUM);
    assertEquals(columnTesterTable.getColumns().get(18).getEnumType(), "TestEnumType");
    assertEquals(columnTesterTable.getColumns().get(19).getName(), "text");
    assertEquals(columnTesterTable.getColumns().get(19).getType(), ColumnType.TEXT);
    assertEquals(columnTesterTable.getColumns().get(20).getName(), "binary");
    assertEquals(columnTesterTable.getColumns().get(20).getType(), ColumnType.BINARY);
    assertEquals(columnTesterTable.getColumns().get(21).getName(), "uuid");
    assertEquals(columnTesterTable.getColumns().get(21).getType(), ColumnType.UUID);
    assertEquals(columnTesterTable.getColumns().get(22).getName(), "json");
    assertEquals(columnTesterTable.getColumns().get(22).getType(), ColumnType.JSON);
    assertEquals(columnTesterTable.getColumns().get(23).getName(), "citext");
    assertEquals(columnTesterTable.getColumns().get(23).getType(), ColumnType.CITEXT);
    assertEquals(columnTesterTable.getColumns().get(24).getName(), "cstext");
    assertEquals(columnTesterTable.getColumns().get(24).getType(), ColumnType.CSTEXT);
    assertEquals(columnTesterTable.getOptions().size(), 3);
    assertTrue(columnTesterTable.hasOption(TableOption.COMPRESS));
    assertTrue(columnTesterTable.hasOption(TableOption.DATA));
    assertTrue(columnTesterTable.hasOption(TableOption.NO_EXPORT));
    assertEquals(columnTesterTable.getLockEscalation(), LockEscalation.DISABLE);
    assertEquals(columnTesterTable.getExportDateColumn(), "byte");
  }

  private void verifyKBITable(Table kbiTable) {
    assertEquals(kbiTable.getRelations().size(), 3);
    assertEquals(kbiTable.getRelations().get(0).getFromTableName(), "KBI");
    assertEquals(kbiTable.getRelations().get(0).getFromColumnName(), "PropertyID");
    assertEquals(kbiTable.getRelations().get(0).getToTableName(), "Property");
    assertEquals(kbiTable.getRelations().get(0).getToColumnName(), "ID");
    assertEquals(kbiTable.getRelations().get(0).getType(), RelationType.CASCADE);

    assertEquals(kbiTable.getRelations().get(1).getFromTableName(), "KBI");
    assertEquals(kbiTable.getRelations().get(1).getFromColumnName(), "UnitID");
    assertEquals(kbiTable.getRelations().get(1).getToTableName(), "Unit");
    assertEquals(kbiTable.getRelations().get(1).getToColumnName(), "ID");
    assertEquals(kbiTable.getRelations().get(1).getType(), RelationType.SETNULL);

    assertEquals(kbiTable.getRelations().get(2).getFromTableName(), "KBI");
    assertEquals(kbiTable.getRelations().get(2).getFromColumnName(), "MasterKBICodeID");
    assertEquals(kbiTable.getRelations().get(2).getToTableName(), "MasterKBICode");
    assertEquals(kbiTable.getRelations().get(2).getToColumnName(), "ID");
    assertEquals(kbiTable.getRelations().get(2).getType(), RelationType.SETNULL);
  }
}
