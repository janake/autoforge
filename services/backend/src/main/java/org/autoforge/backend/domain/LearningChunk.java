package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "learning_chunks")
public class LearningChunk {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "job_id", nullable = false, length = 36)
  private String jobId;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(name = "content", nullable = false, length = 4000)
  private String content;

  @Column(name = "token_estimate", nullable = false)
  private int tokenEstimate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LearningChunk() {
  }

  private LearningChunk(String jobId, String materialId, String ownerSubject, int chunkIndex, String content, int tokenEstimate) {
    this.jobId = jobId;
    this.materialId = materialId;
    this.ownerSubject = ownerSubject;
    this.chunkIndex = chunkIndex;
    this.content = content;
    this.tokenEstimate = tokenEstimate;
  }

  public static LearningChunk create(String jobId, String materialId, String ownerSubject, int chunkIndex, String content, int tokenEstimate) {
    return new LearningChunk(jobId, materialId, ownerSubject, chunkIndex, content, tokenEstimate);
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

  public String getJobId() {
    return jobId;
  }

  public String getMaterialId() {
    return materialId;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public String getContent() {
    return content;
  }

  public int getTokenEstimate() {
    return tokenEstimate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
