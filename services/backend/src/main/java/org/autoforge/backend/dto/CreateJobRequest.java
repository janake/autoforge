package org.autoforge.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
  @NotBlank String jiraIssueKey,
  @NotBlank String prompt,
  @NotBlank String targetRepository,
  @NotBlank String baseBranch
) {
}
