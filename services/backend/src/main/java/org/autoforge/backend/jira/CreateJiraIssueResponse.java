package org.autoforge.backend.jira;

public record CreateJiraIssueResponse(
  String issueKey,
  String issueUrl
) {
}
