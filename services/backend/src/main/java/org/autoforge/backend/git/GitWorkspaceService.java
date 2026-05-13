package org.autoforge.backend.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class GitWorkspaceService {

  public GitWorkspace createWorkspace() {
    try {
      Path workspaceDirectory = Files.createTempDirectory("autoforge-git-workspace-");
      return new GitWorkspace(workspaceDirectory);
    } catch (IOException exception) {
      throw new GitBrokerException("Failed to create git workspace", exception);
    }
  }

  public static final class GitWorkspace implements AutoCloseable {

    private final Path workspaceDirectory;

    private GitWorkspace(Path workspaceDirectory) {
      this.workspaceDirectory = workspaceDirectory;
    }

    public Path workspaceDirectory() {
      return workspaceDirectory;
    }

    @Override
    public void close() {
      GitPaths.deleteRecursively(workspaceDirectory);
    }
  }
}
