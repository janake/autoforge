package org.autoforge.backend.git;

public record CreatePullRequestResponse(
  String prUrl,
  String branchName,
  String commitSha
) {
}
