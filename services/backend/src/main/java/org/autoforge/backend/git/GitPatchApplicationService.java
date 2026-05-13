package org.autoforge.backend.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GitPatchApplicationService {

  public GitCommitResult applyPatchAndCommit(
    GitRepositoryPreparationService.GitRepositoryWorkspace workspace,
    CreatePullRequestRequest request
  ) {
    Path patchFile = writePatchFile(workspace.workspace().workspaceDirectory(), request.patch());

    GitProcessRunner.run(workspace.repositoryDirectory(), "git", "apply", patchFile.toString());

    String status = GitProcessRunner.run(workspace.repositoryDirectory(), "git", "status", "--porcelain").output();
    if (status.isBlank()) {
      throw new GitBrokerException("Patch applied without repository changes");
    }

    GitProcessRunner.run(workspace.repositoryDirectory(), "git", "add", "-A");
    GitProcessRunner.run(
      workspace.repositoryDirectory(),
      "git",
      "-c",
      "user.name=Autoforge Bot",
      "-c",
      "user.email=autoforge-bot@example.com",
      "commit",
      "-m",
      request.commitMessage()
    );

    String commitSha = GitProcessRunner.run(workspace.repositoryDirectory(), "git", "rev-parse", "HEAD").output();
    return new GitCommitResult(commitSha, List.of(status.split("\\R")));
  }

  private Path writePatchFile(Path workspaceDirectory, String patch) {
    try {
      Path patchFile = Files.createTempFile(workspaceDirectory, "autoforge-patch-", ".diff");
      Files.writeString(patchFile, patch, StandardCharsets.UTF_8);
      return patchFile;
    } catch (IOException exception) {
      throw new GitBrokerException("Failed to write patch file", exception);
    }
  }

  public record GitCommitResult(String commitSha, List<String> changedFiles) {
  }
}
