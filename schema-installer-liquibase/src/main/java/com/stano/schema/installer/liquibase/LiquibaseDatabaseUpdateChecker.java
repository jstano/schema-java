package com.stano.schema.installer.liquibase;

import java.sql.Connection;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.executor.ExecutorService;

public class LiquibaseDatabaseUpdateChecker {
  private LiquibaseFactory liquibaseFactory = new LiquibaseFactory();

  public List<String> getPendingMigrations(String changeLogResource, Connection connection) {
    try {
      return getPendingMigrations(
          liquibaseFactory.createLiquibase(changeLogResource, connection),
          liquibaseFactory.getExecutorService());
    } catch (Exception x) {
      throw new LiquibaseRuntimeException(x);
    }
  }

  public List<String> getPendingMigrations(Liquibase liquibase, ExecutorService executorService) {
    try {
      return listUnrunWithChecksumRetry(liquibase).stream().map(ChangeSet::toString).toList();
    } catch (Exception x) {
      throw new LiquibaseRuntimeException(x);
    } finally {
      executorService.clearExecutor("jdbc", liquibase.getDatabase());
    }
  }

  private List<ChangeSet> listUnrunWithChecksumRetry(Liquibase liquibase) throws Exception {
    try {
      return liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
    } catch (Exception x) {
      liquibase.clearCheckSums();
      return liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
    }
  }
}
