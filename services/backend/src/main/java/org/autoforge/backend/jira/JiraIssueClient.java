package org.autoforge.backend.jira;

public interface JiraIssueClient {

  CreateJiraIssueResponse createIssue(CreateJiraIssueRequest request);
}
