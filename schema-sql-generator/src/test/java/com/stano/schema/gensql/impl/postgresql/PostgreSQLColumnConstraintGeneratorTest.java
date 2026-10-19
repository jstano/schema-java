package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQLColumnConstraintGenerator")
class PostgreSQLColumnConstraintGeneratorTest {

  private final PostgreSQLColumnConstraintGenerator generator = createGenerator();

  private static PostgreSQLColumnConstraintGenerator createGenerator() {
    SQLGeneratorOptions options = mock(SQLGeneratorOptions.class);
    when(options.getDatabaseType()).thenReturn(DatabaseType.POSTGRESQL);
    SQLGenerator sqlGenerator = mock(SQLGenerator.class);
    when(sqlGenerator.getSqlGeneratorOptions()).thenReturn(options);
    return new PostgreSQLColumnConstraintGenerator(sqlGenerator);
  }

  @Test
  @DisplayName("a varchar column keeps its user check instead of a redundant length check")
  void varcharKeepsUserCheck() {
    Column column =
        new Column(
            "code",
            ColumnType.VARCHAR,
            6,
            0,
            false,
            "check(code = 'ABC123')",
            null,
            null,
            null,
            null,
            null,
            null);

    assertEquals("check(code = 'ABC123')", generator.getCheckConstraintSQL(column));
  }
}
