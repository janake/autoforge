package org.autoforge.backend.jira;

import java.util.List;

public record CreateJiraIssueRequest(
  String projectKey,
  String issueType,
  String summary,
  String description,
  List<String> labels
) {
}
