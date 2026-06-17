package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.EnumType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PostgreSQL column type SQL generation")
class PostgreSQLColumnTypeGeneratorTest {

  private PostgreSQLColumnTypeGenerator generator;

  @BeforeEach
  void setUp() {
    generator = createGenerator(BooleanMode.NATIVE, null);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("simple types should map to expected SQL")
  @CsvSource({
    "SEQUENCE,    serial",
    "LONGSEQUENCE, bigserial",
    "BYTE,        smallint",
    "SHORT,       smallint",
    "INT,         integer",
    "LONG,        bigint",
    "FLOAT,       real",
    "DOUBLE,      double precision",
    "DATE,        date",
    "DATETIME,    timestamp",
    "TIMESTAMP,   timestamp",
    "TIME,        time",
    "TIMESTAMPTZ, timestamptz",
    "UUID,        uuid",
    "BINARY,      bytea",
    "JSON,        jsonb",
    "CITEXT,      citext",
    "CSTEXT,      text",
  })
  void simpleTypeShouldMapToExpectedSql(String typeName, String expectedSql) {
    Column col = new Column("col", ColumnType.getColumnType(typeName.trim()), 0, false);
    assertEquals(expectedSql.trim(), generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("CHAR with length should produce char(n)")
  void charShouldProduceCharWithLength() {
    Column col = new Column("col", ColumnType.CHAR, 10, false);
    assertEquals("char(10)", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("VARCHAR should always produce text regardless of length")
  void varcharShouldProduceText() {
    Column col = new Column("col", ColumnType.VARCHAR, 255, false);
    assertEquals("text", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("DECIMAL with precision and scale should produce decimal(p,s)")
  void decimalWithPrecisionAndScaleShouldProduceDecimalPrecisionScale() {
    Column col =
        new Column(
            "col", ColumnType.DECIMAL, 19, 4, false, null, null, null, null, null, null, null);
    assertEquals("decimal(19,4)", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("DECIMAL with no precision or scale should produce decimal")
  void decimalWithNoPrecisionOrScaleShouldProduceBareDecimal() {
    Column col =
        new Column(
            "col", ColumnType.DECIMAL, 0, 0, false, null, null, null, null, null, null, null);
    assertEquals("decimal", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("BOOLEAN with NATIVE mode should produce boolean")
  void booleanNativeShouldProduceBoolean() {
    Column col = new Column("col", ColumnType.BOOLEAN, 0, false);
    assertEquals("boolean", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("BOOLEAN with YES_NO mode should produce varchar(3)")
  void booleanYesNoShouldProduceVarchar3() {
    PostgreSQLColumnTypeGenerator yesNoGenerator = createGenerator(BooleanMode.YES_NO, null);
    Column col = new Column("col", ColumnType.BOOLEAN, 0, false);
    assertEquals("varchar(3)", yesNoGenerator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("BOOLEAN with YN mode should produce char(1)")
  void booleanYnShouldProduceChar1() {
    PostgreSQLColumnTypeGenerator ynGenerator = createGenerator(BooleanMode.YN, null);
    Column col = new Column("col", ColumnType.BOOLEAN, 0, false);
    assertEquals("char(1)", ynGenerator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("TEXT with case-insensitive schema should produce citext")
  void textWithCaseInsensitiveSchemaShouldProduceCitext() {
    Schema schema = new Schema(null);
    schema.setCaseSensitiveText(false);
    PostgreSQLColumnTypeGenerator caseInsensitiveGenerator =
        createGenerator(BooleanMode.NATIVE, schema);
    Column col = new Column("col", ColumnType.TEXT, 0, false);
    assertEquals("citext", caseInsensitiveGenerator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("TEXT with case-sensitive schema should produce text")
  void textWithCaseSensitiveSchemaShouldProduceText() {
    Schema schema = new Schema(null);
    schema.setCaseSensitiveText(true);
    PostgreSQLColumnTypeGenerator caseSensitiveGenerator =
        createGenerator(BooleanMode.NATIVE, schema);
    Column col = new Column("col", ColumnType.TEXT, 0, false);
    assertEquals("text", caseSensitiveGenerator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("ENUM should produce snake_case of enum type name")
  void enumShouldProduceSnakeCaseTypeName() {
    Schema schema = new Schema(null);
    EnumType enumType = new EnumType("UserStatus");
    enumType.addValue(new EnumValue("Active", "ACTIVE"));
    schema.addEnumType(enumType);
    PostgreSQLColumnTypeGenerator enumGenerator = createGenerator(BooleanMode.NATIVE, schema);
    Column col =
        new Column(
            "col", ColumnType.ENUM, 0, 0, false, null, null, null, null, null, "UserStatus", null);
    assertEquals("user_status", enumGenerator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("ARRAY of INT should produce integer[]")
  void arrayOfIntShouldProduceIntegerArray() {
    Column col =
        new Column(
            "col",
            ColumnType.ARRAY,
            0,
            0,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            ColumnType.INT);
    assertEquals("integer[]", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("ARRAY of VARCHAR should produce text[]")
  void arrayOfVarcharShouldProduceTextArray() {
    Column col =
        new Column(
            "col",
            ColumnType.ARRAY,
            100,
            0,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            ColumnType.VARCHAR);
    assertEquals("text[]", generator.getColumnTypeSql(null, col));
  }

  @Test
  @DisplayName("ARRAY of LONG should produce bigint[]")
  void arrayOfLongShouldProduceBigintArray() {
    Column col =
        new Column(
            "col",
            ColumnType.ARRAY,
            0,
            0,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            ColumnType.LONG);
    assertEquals("bigint[]", generator.getColumnTypeSql(null, col));
  }

  private PostgreSQLColumnTypeGenerator createGenerator(BooleanMode booleanMode, Schema schema) {
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                schema,
                null,
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.RELATIONS,
                booleanMode,
                OutputMode.ALL)) {
          @Override
          protected void outputTables() {}

          @Override
          protected void outputRelations() {}

          @Override
          protected void outputIndexes() {}

          @Override
          protected void outputTriggers() {}

          @Override
          protected void outputFunctions() {}

          @Override
          protected void outputViews() {}

          @Override
          protected void outputProcedures() {}

          @Override
          protected void outputOtherSqlTop() {}

          @Override
          protected void outputOtherSqlBottom() {}
        };
    return new PostgreSQLColumnTypeGenerator(sqlGen);
  }
}
