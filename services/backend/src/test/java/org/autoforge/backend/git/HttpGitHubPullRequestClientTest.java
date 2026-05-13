package org.autoforge.backend.git;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HttpGitHubPullRequestClientTest {

  @Test
  void rejectsMissingGitHubTokenWithoutLoggingSecretValue() {
    var client = new HttpGitHubPullRequestClient(
      new GitHubRepositoryCoordinates("org", "repo"),
      "https://api.github.com",
      ""
    );

    assertThatThrownBy(() -> client.createPullRequest(new CreatePullRequestRequest(
      "https://github.com/org/repo.git",
      "main",
      "autoforge/AUTO-235-security-baseline",
      "AUTO-235 Security baseline",
      "AUTO-235 Security baseline",
      null,
      "diff --git a/README.md b/README.md"
    )))
      .isInstanceOf(GitBrokerException.class)
      .hasMessage("Missing AUTOFORGE_GITHUB_TOKEN configuration");
  }
}
