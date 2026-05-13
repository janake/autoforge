package org.autoforge.backend.git;

public interface GitHubPullRequestClient {

  GitHubPullRequestResponse createPullRequest(CreatePullRequestRequest request);

  void pushBranch(CreatePullRequestRequest request, GitRepositoryPreparationService.GitRepositoryWorkspace workspace);
}
