package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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

  @Lob
  @Column(name = "structured_content")
  private String structuredContent;

  @Column(name = "source_references", nullable = false, length = 4000)
  private String sourceReferences;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_set_status", length = 32)
  private LearningQuestionSetStatus questionSetStatus;

  @Column(name = "fallback_used", nullable = false)
  private boolean fallbackUsed;

  @Column(name = "fallback_reason", length = 1000)
  private String fallbackReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "generation_status", nullable = false, length = 32)
  private LearningGenerationStatus generationStatus;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

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
    String structuredContent,
    String sourceReferences,
    LearningQuestionSetStatus questionSetStatus,
    boolean fallbackUsed,
    String fallbackReason,
    LearningGenerationStatus generationStatus,
    String errorMessage
  ) {
    this.materialId = materialId;
    this.ownerSubject = ownerSubject;
    this.generationType = generationType;
    this.content = content;
    this.structuredContent = structuredContent;
    this.sourceReferences = sourceReferences;
    this.questionSetStatus = questionSetStatus;
    this.fallbackUsed = fallbackUsed;
    this.fallbackReason = fallbackReason;
    this.generationStatus = generationStatus;
    this.errorMessage = errorMessage;
  }

  public static LearningGeneratedContent create(
    String materialId,
    String ownerSubject,
    LearningContentGenerationType generationType,
    String content,
    String structuredContent,
    String sourceReferences,
    LearningQuestionSetStatus questionSetStatus,
    boolean fallbackUsed,
    String fallbackReason,
    LearningGenerationStatus generationStatus,
    String errorMessage
  ) {
    return new LearningGeneratedContent(
      materialId,
      ownerSubject,
      generationType,
      content,
      structuredContent,
      sourceReferences,
      questionSetStatus,
      fallbackUsed,
      fallbackReason,
      generationStatus,
      errorMessage
    );
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

  public String getStructuredContent() {
    return structuredContent;
  }

  public String getSourceReferences() {
    return sourceReferences;
  }

  public LearningQuestionSetStatus getQuestionSetStatus() {
    return questionSetStatus;
  }

  public void setQuestionSetStatus(LearningQuestionSetStatus questionSetStatus) {
    this.questionSetStatus = questionSetStatus;
  }

  public boolean isFallbackUsed() {
    return fallbackUsed;
  }

  public String getFallbackReason() {
    return fallbackReason;
  }

  public LearningGenerationStatus getGenerationStatus() {
    return generationStatus;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
