package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.View;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ViewGenerator extends BaseGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ViewGenerator.class);

  public ViewGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  public void outputViews() {

    viewsInDependencyOrder(schema.getViews(databaseType))
        .forEach(
            view -> {
              LOGGER.debug("Generating SQL for view " + getFullyQualifiedViewName(view));

              outputView(view);
            });
  }

  /**
   * Orders {@code views} so that a view whose SQL references another view comes after it; emitting
   * in declaration order fails when a view selects from a later-declared view.
   *
   * <p>View bodies are opaque SQL, so dependencies are found by matching each view's
   * (case-insensitive) name against the identifier tokens of the other views' SQL — quoting and
   * schema qualification don't hide a name, and a name inside a longer identifier doesn't match.
   * Views with no dependency between them keep their declaration order. Views caught in a cycle
   * fall back to declaration order after everything orderable, since no ordering satisfies them.
   */
  static List<View> viewsInDependencyOrder(List<View> views) {
    int count = views.size();
    List<Set<String>> tokens = views.stream().map(view -> identifierTokens(view.getSql())).toList();
    List<String> names = views.stream().map(view -> view.getName().toLowerCase()).toList();

    boolean[] emitted = new boolean[count];
    List<View> ordered = new ArrayList<>(count);

    boolean progressed = true;
    while (progressed) {
      progressed = false;

      for (int i = 0; i < count; i++) {
        if (!emitted[i] && dependenciesEmitted(i, count, tokens, names, emitted)) {
          emitted[i] = true;
          ordered.add(views.get(i));
          progressed = true;
          break;
        }
      }
    }

    for (int i = 0; i < count; i++) {
      if (!emitted[i]) {
        ordered.add(views.get(i));
      }
    }

    return ordered;
  }

  private static boolean dependenciesEmitted(
      int index, int count, List<Set<String>> tokens, List<String> names, boolean[] emitted) {
    for (int other = 0; other < count; other++) {
      if (other != index && !emitted[other] && tokens.get(index).contains(names.get(other))) {
        return false;
      }
    }

    return true;
  }

  /** Splits {@code sql} into lower-case identifier-like tokens (letters, digits, {@code _ $}). */
  private static Set<String> identifierTokens(String sql) {
    Set<String> tokens = new HashSet<>();

    for (String token : sql.split("[^\\p{L}\\p{N}_$]+")) {
      if (!token.isEmpty()) {
        tokens.add(token.toLowerCase());
      }
    }

    return tokens;
  }

  protected abstract void outputView(View view);
}
