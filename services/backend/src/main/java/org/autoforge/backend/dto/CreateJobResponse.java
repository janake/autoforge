package org.autoforge.backend.dto;

public record CreateJobResponse(
  String jobId,
  String jiraIssueKey,
  String status
) {
}
