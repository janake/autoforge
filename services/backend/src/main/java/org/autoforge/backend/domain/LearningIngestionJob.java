package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "learning_ingestion_jobs")
public class LearningIngestionJob {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, unique = true, length = 36)
  private String materialId;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private LearningIngestionStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "last_error", length = 4000)
  private String lastError;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LearningIngestionJob() {
  }

  public LearningIngestionJob(String materialId, String ownerSubject, LearningIngestionStatus status) {
    this.materialId = materialId;
    this.ownerSubject = ownerSubject;
    this.status = status;
  }

  public static LearningIngestionJob createQueued(String materialId, String ownerSubject) {
    return new LearningIngestionJob(materialId, ownerSubject, LearningIngestionStatus.QUEUED);
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

  public LearningIngestionStatus getStatus() {
    return status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markProcessing() {
    this.status = LearningIngestionStatus.PROCESSING;
    this.lastError = null;
  }

  public void markCompleted() {
    this.status = LearningIngestionStatus.COMPLETED;
    this.lastError = null;
  }

  public void markFailed(String errorMessage) {
    this.status = LearningIngestionStatus.FAILED;
    this.lastError = errorMessage;
  }

  public void incrementRetryCount() {
    this.retryCount += 1;
  }
}
