package com.stano.schema.git;

import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Reads a schema source that is either a plain file path or a git {@code <rev>:<path>} reference
 * (e.g. {@code HEAD:schema.xml}, {@code v1.2.0:schema/schema.xml}), parsing it into a {@link
 * Schema}.
 *
 * <p>A value is only treated as a git reference when it contains a {@code :} <em>and</em> the part
 * before the first {@code :} resolves to a real commit in a repository discoverable from the search
 * start directory (the current working directory, by default) - this keeps plain file paths
 * (including Windows drive letters like {@code C:\schema.xml}) working unchanged. If that directory
 * isn't inside a git repository, or the given revision doesn't resolve, the source is treated as a
 * plain file path too, so the resulting "file not found" error stays accurate about what was
 * actually tried.
 */
public class GitSchemaSourceReader {
  private final Path searchStartDirectory;

  /** Creates a reader that discovers the git repository from the current working directory. */
  public GitSchemaSourceReader() {
    this(Path.of("").toAbsolutePath());
  }

  /**
   * Creates a reader that discovers the git repository starting from the given directory, rather
   * than the current working directory.
   *
   * @param searchStartDirectory the directory to start searching for an enclosing git repository
   *     from
   */
  public GitSchemaSourceReader(Path searchStartDirectory) {
    this.searchStartDirectory = searchStartDirectory;
  }

  /**
   * Reads and parses the given schema source.
   *
   * @param source a file path, or a git {@code <rev>:<path>} reference
   * @return the parsed {@link Schema}
   * @throws IOException if the underlying file or git blob cannot be read, or the schema cannot be
   *     parsed
   * @throws IllegalStateException if {@code source} looks like a git reference that was resolved
   *     during the availability check, but the referenced path is not present at that revision
   */
  public Schema readSchema(String source) throws IOException {
    GitRef gitRef = parseGitRef(source);
    if (gitRef != null) {
      return readFromGit(gitRef, source);
    }

    return new SchemaParser().parseSchema(Path.of(source).toUri().toURL());
  }

  private record GitRef(String rev, String path) {}

  private GitRef parseGitRef(String source) {
    int colonIndex = source.indexOf(':');
    if (colonIndex <= 0 || colonIndex == source.length() - 1) {
      return null;
    }

    String rev = source.substring(0, colonIndex);
    String path = source.substring(colonIndex + 1);

    try (Repository repository = openRepository()) {
      if (repository == null || resolveCommit(repository, rev) == null) {
        return null;
      }
      return new GitRef(rev, path);
    } catch (IOException e) {
      return null;
    }
  }

  private Schema readFromGit(GitRef gitRef, String source) throws IOException {
    try (Repository repository = openRepository()) {
      if (repository == null) {
        throw new IllegalStateException("Not inside a git repository, reading: " + source);
      }

      ObjectId revId = resolveCommit(repository, gitRef.rev());
      if (revId == null) {
        throw new IllegalStateException(
            "Cannot resolve git revision '" + gitRef.rev() + "' for: " + source);
      }

      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(revId);

        try (TreeWalk treeWalk = TreeWalk.forPath(repository, gitRef.path(), commit.getTree())) {
          if (treeWalk == null) {
            throw new IllegalStateException(
                "Path '"
                    + gitRef.path()
                    + "' was not found at revision '"
                    + gitRef.rev()
                    + "', reading: "
                    + source);
          }

          ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
          URL schemaURL = Path.of(gitRef.path()).toUri().toURL();

          try (InputStream inputStream = loader.openStream()) {
            return new SchemaParser().parseSchema(schemaURL, inputStream);
          }
        }
      }
    }
  }

  private ObjectId resolveCommit(Repository repository, String rev) throws IOException {
    return repository.resolve(rev + "^{commit}");
  }

  private Repository openRepository() {
    try {
      return new FileRepositoryBuilder()
          .findGitDir(searchStartDirectory.toFile())
          .readEnvironment()
          .build();
    } catch (IOException | IllegalArgumentException e) {
      return null;
    }
  }
}
