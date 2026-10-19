package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stano.schema.model.View;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ViewGenerator dependency ordering")
class ViewGeneratorTest {

  private static View view(String name, String sql) {
    return new View(null, name, sql, null);
  }

  private static List<String> names(List<View> views) {
    return views.stream().map(View::getName).toList();
  }

  @Test
  @DisplayName("a view selecting from a later-declared view is moved after it")
  void viewSelectingFromLaterDeclaredViewIsMovedAfterIt() {
    List<View> ordered =
        ViewGenerator.viewsInDependencyOrder(
            List.of(view("b", "select * from a"), view("a", "select 1 from users")));

    assertEquals(List.of("a", "b"), names(ordered));
  }

  @Test
  @DisplayName("independent views keep declaration order")
  void independentViewsKeepDeclarationOrder() {
    List<View> ordered =
        ViewGenerator.viewsInDependencyOrder(List.of(view("z", "select 1"), view("a", "select 2")));

    assertEquals(List.of("z", "a"), names(ordered));
  }

  @Test
  @DisplayName("chains are ordered through quoting, schema qualification and case")
  void handlesChainsQuotingQualificationAndCase() {
    List<View> ordered =
        ViewGenerator.viewsInDependencyOrder(
            List.of(
                view("c", "select * from public.\"B\""),
                view("b", "select * from [dbo].A"),
                view("a", "select 1")));

    assertEquals(List.of("a", "b", "c"), names(ordered));
  }

  @Test
  @DisplayName("a name inside a longer identifier is not a dependency")
  void nameInsideLongerIdentifierIsNotADependency() {
    List<View> ordered =
        ViewGenerator.viewsInDependencyOrder(
            List.of(view("x", "select * from a_users"), view("a", "select 1")));

    assertEquals(List.of("x", "a"), names(ordered));
  }

  @Test
  @DisplayName("a cycle falls back to declaration order after the orderable views")
  void cycleFallsBackToDeclarationOrder() {
    List<View> ordered =
        ViewGenerator.viewsInDependencyOrder(
            List.of(
                view("a", "select * from b"), view("b", "select * from a"), view("c", "select 1")));

    assertEquals(List.of("c", "a", "b"), names(ordered));
  }
}
