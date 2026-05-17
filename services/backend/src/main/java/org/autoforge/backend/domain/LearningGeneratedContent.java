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
@Table(name = "learning_generated_content")
public class LearningGeneratedContent {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Enumerated(EnumType.STRING)
  @Column(name = "generation_type", nullable = false, length = 32)
  private LearningContentGenerationType generationType;

  @Column(name = "content", nullable = false, length = 4000)
  private String content;

  @Column(name = "source_references", nullable = false, length = 4000)
  private String sourceReferences;

  @Column(name = "fallback_used", nullable = false)
  private boolean fallbackUsed;

  @Column(name = "fallback_reason", length = 1000)
  private String fallbackReason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LearningGeneratedContent() {
  }

  private LearningGeneratedContent(
    String materialId,
    String ownerSubject,
    LearningContentGenerationType generationType,
    String content,
    String sourceReferences,
    boolean fallbackUsed,
    String fallbackReason
  ) {
    this.materialId = materialId;
    this.ownerSubject = ownerSubject;
    this.generationType = generationType;
    this.content = content;
    this.sourceReferences = sourceReferences;
    this.fallbackUsed = fallbackUsed;
    this.fallbackReason = fallbackReason;
  }

  public static LearningGeneratedContent create(
    String materialId,
    String ownerSubject,
    LearningContentGenerationType generationType,
    String content,
    String sourceReferences,
    boolean fallbackUsed,
    String fallbackReason
  ) {
    return new LearningGeneratedContent(materialId, ownerSubject, generationType, content, sourceReferences, fallbackUsed, fallbackReason);
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

  public String getMaterialId() {
    return materialId;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public LearningContentGenerationType getGenerationType() {
    return generationType;
  }

  public String getContent() {
    return content;
  }

  public String getSourceReferences() {
    return sourceReferences;
  }

  public boolean isFallbackUsed() {
    return fallbackUsed;
  }

  public String getFallbackReason() {
    return fallbackReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
