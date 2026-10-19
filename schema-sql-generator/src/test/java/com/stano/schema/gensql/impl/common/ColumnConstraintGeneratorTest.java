package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ColumnConstraintGenerator check combination")
class ColumnConstraintGeneratorTest {

  private static ColumnConstraintGenerator generator(BooleanMode booleanMode) {
    SQLGeneratorOptions options = mock(SQLGeneratorOptions.class);
    when(options.getDatabaseType()).thenReturn(DatabaseType.POSTGRESQL);
    when(options.getBooleanMode()).thenReturn(booleanMode);
    SQLGenerator sqlGenerator = mock(SQLGenerator.class);
    when(sqlGenerator.getSqlGeneratorOptions()).thenReturn(options);
    return new ColumnConstraintGenerator(sqlGenerator);
  }

  private static Column column(
      String name, ColumnType type, String check, String minValue, String maxValue) {
    return new Column(name, type, 0, 0, false, check, null, null, minValue, maxValue, null, null);
  }

  @Test
  @DisplayName("a user check and min/max are combined instead of the min/max being dropped")
  void userCheckAndMinMaxAreCombined() {
    Column amount = column("amount", ColumnType.INT, "amount <> 13", "0", "100");

    assertEquals(
        "check((amount <> 13) and (amount >= 0 and amount <= 100))",
        generator(BooleanMode.NATIVE).getCheckConstraintSQL(amount));
  }

  @Test
  @DisplayName("an already-wrapped user check is unwrapped before combining with min/max")
  void wrappedUserCheckIsUnwrappedBeforeCombining() {
    Column amount = column("amount", ColumnType.INT, "check(amount <> 13)", "0", null);

    assertEquals(
        "check((amount <> 13) and (amount >= 0))",
        generator(BooleanMode.NATIVE).getCheckConstraintSQL(amount));
  }

  @Test
  @DisplayName("a boolean column with min/max keeps the min/max under native boolean mode")
  void booleanMinMaxUnderNativeMode() {
    Column flag = column("flag", ColumnType.BOOLEAN, null, "0", "1");

    assertEquals(
        "check(flag >= 0 and flag <= 1)",
        generator(BooleanMode.NATIVE).getCheckConstraintSQL(flag));
  }

  @Test
  @DisplayName("a boolean column with min/max combines both clauses under yes/no mode")
  void booleanMinMaxUnderYesNoMode() {
    Column flag = column("flag", ColumnType.BOOLEAN, null, "0", null);

    assertEquals(
        "check((flag in ('Yes','No')) and (flag >= 0))",
        generator(BooleanMode.YES_NO).getCheckConstraintSQL(flag));
  }
}
