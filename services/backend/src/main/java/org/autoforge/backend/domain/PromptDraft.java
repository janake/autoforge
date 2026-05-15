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
@Table(name = "prompt_drafts")
public class PromptDraft {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "prompt", nullable = false, length = 4000)
  private String prompt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private PromptDraftStatus status;

  @Column(name = "pending_questions", length = 4000)
  private String pendingQuestions;

  @Column(name = "approved_by", length = 128)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PromptDraft() {
  }

  public PromptDraft(String prompt, PromptDraftStatus status, String pendingQuestions) {
    this.prompt = prompt;
    this.status = status;
    this.pendingQuestions = pendingQuestions;
  }

  public static PromptDraft create(String prompt) {
    return new PromptDraft(prompt, PromptDraftStatus.DRAFT, null);
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

  public String getPrompt() {
    return prompt;
  }

  public PromptDraftStatus getStatus() {
    return status;
  }

  public String getPendingQuestions() {
    return pendingQuestions;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public void setStatus(PromptDraftStatus status) {
    this.status = status;
  }

  public void setPendingQuestions(String pendingQuestions) {
    this.pendingQuestions = pendingQuestions;
  }

  public void approve(String approvedBy, Instant approvedAt) {
    this.status = PromptDraftStatus.APPROVED;
    this.approvedBy = approvedBy;
    this.approvedAt = approvedAt;
  }
}
