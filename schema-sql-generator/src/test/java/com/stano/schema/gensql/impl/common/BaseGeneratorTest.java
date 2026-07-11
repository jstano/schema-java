package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("BaseGenerator key name truncation")
class BaseGeneratorTest {

  @Test
  @DisplayName("untruncated short name should return unchanged")
  void untruncatedShortNameShouldReturnUnchanged() {
    BaseGenerator gen = createGenerator(DatabaseType.SQL_SERVER);
    String result = gen.buildKeyName("ak_", "short", "1");
    assertEquals("ak_short1", result);
  }

  @ParameterizedTest(name = "database {0} with single-digit suffix")
  @DisplayName("single-digit suffix should truncate to maxKeyNameLength")
  @CsvSource({"SQL_SERVER,32", "POSTGRESQL,63", "H2,64"})
  void singleDigitSuffixShouldTruncateToMax(DatabaseType databaseType, int maxKeyNameLength) {
    BaseGenerator gen = createGenerator(databaseType);
    String longTableName = "a".repeat(maxKeyNameLength);
    String result = gen.buildKeyName("ak_", longTableName, "5");

    assertTrue(
        result.length() <= maxKeyNameLength,
        "Result should not exceed maxKeyNameLength: "
            + result.length()
            + " <= "
            + maxKeyNameLength);
    assertEquals(
        maxKeyNameLength,
        result.length(),
        "When truncation occurs, result should be exactly maxKeyNameLength");
  }

  @ParameterizedTest(name = "database {0} with double-digit suffix")
  @DisplayName("double-digit suffix should truncate to maxKeyNameLength (regression fix)")
  @CsvSource({"SQL_SERVER,32", "POSTGRESQL,63", "H2,64"})
  void doubleDigitSuffixShouldTruncateToMax(DatabaseType databaseType, int maxKeyNameLength) {
    BaseGenerator gen = createGenerator(databaseType);
    String longTableName = "a".repeat(maxKeyNameLength);
    String result = gen.buildKeyName("ak_", longTableName, "12");

    assertTrue(
        result.length() <= maxKeyNameLength,
        "Result should not exceed maxKeyNameLength: "
            + result.length()
            + " <= "
            + maxKeyNameLength);
  }

  @ParameterizedTest(name = "database {0} with triple-digit suffix")
  @DisplayName("triple-digit suffix should truncate to maxKeyNameLength")
  @CsvSource({"SQL_SERVER,32", "POSTGRESQL,63", "H2,64"})
  void tripleDigitSuffixShouldTruncateToMax(DatabaseType databaseType, int maxKeyNameLength) {
    BaseGenerator gen = createGenerator(databaseType);
    String longTableName = "a".repeat(maxKeyNameLength);
    String result = gen.buildKeyName("ix_", longTableName, "100");

    assertTrue(
        result.length() <= maxKeyNameLength,
        "Result should not exceed maxKeyNameLength: "
            + result.length()
            + " <= "
            + maxKeyNameLength);
  }

  @Test
  @DisplayName("empty suffix (like PK) should truncate correctly")
  void emptySuffixShouldTruncateCorrectly() {
    BaseGenerator gen = createGenerator(DatabaseType.SQL_SERVER);
    String longTableName = "a".repeat(30);
    String result = gen.buildKeyName("pk_", longTableName, "");

    assertEquals(32, result.length());
    assertTrue(result.startsWith("pk_"));
  }

  @Test
  @DisplayName("various prefixes should reserve correct space")
  void variousPrefixesShouldReserveCorrectSpace() {
    BaseGenerator gen = createGenerator(DatabaseType.SQL_SERVER);
    String longTableName = "a".repeat(25);

    String akResult = gen.buildKeyName("ak_", longTableName, "12");
    String ixResult = gen.buildKeyName("ix_", longTableName, "12");
    String fkResult = gen.buildKeyName("fk_", longTableName, "12");
    String pkResult = gen.buildKeyName("pk_", longTableName, "");

    assertTrue(akResult.length() <= 32);
    assertTrue(ixResult.length() <= 32);
    assertTrue(fkResult.length() <= 32);
    assertTrue(pkResult.length() <= 32);
  }

  private BaseGenerator createGenerator(DatabaseType databaseType) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    SQLGenerator sqlGen =
        new SQLGenerator(
            new SQLGeneratorOptions(
                new Schema(null),
                pw,
                databaseType,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
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
    return new BaseGenerator(sqlGen);
  }
}
