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

@Entity
@Table(name = "prompt_draft_messages")
public class PromptDraftMessage {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "draft_id", nullable = false, length = 36)
  private String draftId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 32)
  private PromptDraftMessageRole role;

  @Column(name = "content", nullable = false, length = 4000)
  private String content;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PromptDraftMessage() {
  }

  public PromptDraftMessage(String draftId, PromptDraftMessageRole role, String content) {
    this.draftId = draftId;
    this.role = role;
    this.content = content;
  }

  public static PromptDraftMessage create(String draftId, PromptDraftMessageRole role, String content) {
    return new PromptDraftMessage(draftId, role, content);
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

  public String getDraftId() {
    return draftId;
  }

  public PromptDraftMessageRole getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
