package org.autoforge.backend.git;

import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class GitRepositoryPreparationService {

  private final GitWorkspaceService gitWorkspaceService;

  public GitRepositoryPreparationService(GitWorkspaceService gitWorkspaceService) {
    this.gitWorkspaceService = gitWorkspaceService;
  }

  public GitRepositoryWorkspace prepareRepository(CreatePullRequestRequest request) {
    GitWorkspaceService.GitWorkspace workspace = gitWorkspaceService.createWorkspace();
    Path repositoryDirectory = workspace.workspaceDirectory().resolve("repository");

    try {
      GitProcessRunner.run(
        null,
        "git",
        "clone",
        request.repositoryUrl(),
        repositoryDirectory.toString()
      );

      GitProcessRunner.run(
        repositoryDirectory,
        "git",
        "checkout",
        "-B",
        request.baseBranch(),
        "origin/" + request.baseBranch()
      );

      GitProcessRunner.run(
        repositoryDirectory,
        "git",
        "checkout",
        "-b",
        request.branchName()
      );

      return new GitRepositoryWorkspace(
        workspace,
        repositoryDirectory,
        request.repositoryUrl(),
        request.baseBranch(),
        request.branchName()
      );
    } catch (RuntimeException exception) {
      workspace.close();
      throw exception;
    }
  }

  public record GitRepositoryWorkspace(
    GitWorkspaceService.GitWorkspace workspace,
    Path repositoryDirectory,
    String repositoryUrl,
    String baseBranch,
    String branchName
  ) implements AutoCloseable {

    @Override
    public void close() {
      workspace.close();
    }
  }
}
