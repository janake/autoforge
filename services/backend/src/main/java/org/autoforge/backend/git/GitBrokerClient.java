package org.autoforge.backend.git;

public interface GitBrokerClient {

  CreatePullRequestResponse createPullRequest(CreatePullRequestRequest request);
}
