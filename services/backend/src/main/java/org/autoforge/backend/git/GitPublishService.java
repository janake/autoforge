package org.autoforge.backend.git;

import org.springframework.stereotype.Service;

@Service
public class GitPublishService {

  private final GitHubPullRequestClient gitHubPullRequestClient;

  public GitPublishService(GitHubPullRequestClient gitHubPullRequestClient) {
    this.gitHubPullRequestClient = gitHubPullRequestClient;
  }

  public CreatePullRequestResponse pushBranchAndOpenPr(
    GitRepositoryPreparationService.GitRepositoryWorkspace workspace,
    CreatePullRequestRequest request,
    GitPatchApplicationService.GitCommitResult commitResult
  ) {
    gitHubPullRequestClient.pushBranch(request, workspace);
    GitHubPullRequestResponse pullRequestResponse = gitHubPullRequestClient.createPullRequest(request);
    return new CreatePullRequestResponse(pullRequestResponse.prUrl(), request.branchName(), commitResult.commitSha());
  }
}
