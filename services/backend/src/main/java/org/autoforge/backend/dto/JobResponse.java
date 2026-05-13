package org.autoforge.backend.dto;

import java.time.Instant;

public record JobResponse(
  String jobId,
  String jiraIssueKey,
  String prompt,
  String targetRepository,
  String baseBranch,
  String status,
  String prUrl,
  String errorMessage,
  Instant createdAt,
  Instant updatedAt
) {
}
