package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "jobs")
public class Job {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "jira_issue_key", nullable = false, length = 32, unique = true)
  private String jiraIssueKey;

  @Column(name = "prompt", nullable = false, length = 4000)
  private String prompt;

  @Column(name = "target_repository", nullable = false, length = 512)
  private String targetRepository;

  @Column(name = "base_branch", nullable = false, length = 128)
  private String baseBranch;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private JobStatus status;

  @Column(name = "pr_url", length = 1024)
  private String prUrl;

  @Column(name = "error_message", length = 4000)
  private String errorMessage;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Job() {
  }

  public Job(String jiraIssueKey, String prompt, String targetRepository, String baseBranch, JobStatus status) {
    this.jiraIssueKey = jiraIssueKey;
    this.prompt = prompt;
    this.targetRepository = targetRepository;
    this.baseBranch = baseBranch;
    this.status = status;
  }

  @PrePersist
  void prePersist() {
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
  }

  public String getId() {
    return id;
  }

  public String getJiraIssueKey() {
    return jiraIssueKey;
  }

  public String getPrompt() {
    return prompt;
  }

  public String getTargetRepository() {
    return targetRepository;
  }

  public String getBaseBranch() {
    return baseBranch;
  }

  public JobStatus getStatus() {
    return status;
  }

  public String getPrUrl() {
    return prUrl;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setStatus(JobStatus status) {
    this.status = status;
  }

  public void setPrUrl(String prUrl) {
    this.prUrl = prUrl;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
