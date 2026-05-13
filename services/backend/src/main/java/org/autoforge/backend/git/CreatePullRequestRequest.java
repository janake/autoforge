package org.autoforge.backend.git;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePullRequestRequest(
  @NotBlank @Size(max = 2048) String repositoryUrl,
  @NotBlank @Size(max = 128) String baseBranch,
  @NotBlank @Size(max = 128) String branchName,
  @NotBlank @Size(max = 255) String commitMessage,
  @NotBlank @Size(max = 255) String pullRequestTitle,
  @Size(max = 4000) String pullRequestBody,
  @NotBlank @Size(max = 200_000) String patch
) {
}
