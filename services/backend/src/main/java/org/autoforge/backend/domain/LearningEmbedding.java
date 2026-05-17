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
@Table(name = "learning_embeddings")
public class LearningEmbedding {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "chunk_id", nullable = false, unique = true, length = 36)
  private String chunkId;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Column(name = "model", nullable = false, length = 128)
  private String model;

  @Column(name = "dimensions", nullable = false)
  private int dimensions;

  @Column(name = "vector_json", nullable = false, length = 4000)
  private String vectorJson;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LearningEmbedding() {
  }

  private LearningEmbedding(String chunkId, String ownerSubject, String model, int dimensions, String vectorJson) {
    this.chunkId = chunkId;
    this.ownerSubject = ownerSubject;
    this.model = model;
    this.dimensions = dimensions;
    this.vectorJson = vectorJson;
  }

  public static LearningEmbedding create(String chunkId, String ownerSubject, String model, int dimensions, String vectorJson) {
    return new LearningEmbedding(chunkId, ownerSubject, model, dimensions, vectorJson);
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

  public String getChunkId() {
    return chunkId;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public String getModel() {
    return model;
  }

  public int getDimensions() {
    return dimensions;
  }

  public String getVectorJson() {
    return vectorJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
