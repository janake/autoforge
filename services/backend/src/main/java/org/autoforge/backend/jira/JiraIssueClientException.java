package org.autoforge.backend.jira;

public class JiraIssueClientException extends RuntimeException {

  public JiraIssueClientException(String message) {
    super(message);
  }

  public JiraIssueClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
