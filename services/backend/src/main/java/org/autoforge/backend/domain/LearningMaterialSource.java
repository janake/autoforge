package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "learning_material_sources")
public class LearningMaterialSource {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Column(name = "source_type", nullable = false, length = 64)
  private String sourceType;

  @Column(name = "source_name", nullable = false, length = 256)
  private String sourceName;

  @Column(name = "original_filename", length = 256)
  private String originalFilename;

  @Column(name = "content_type", length = 128)
  private String contentType;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "storage_object_key", nullable = false, length = 512)
  private String storageObjectKey;

  @Column(name = "storage_object_uri", nullable = false, length = 1000)
  private String storageObjectUri;

  @Column(name = "content_hash", nullable = false, length = 128)
  private String contentHash;

  @Column(name = "content_etag", nullable = false, length = 64)
  private String contentETag;

  @Lob
  @Column(name = "content")
  private byte[] content;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LearningMaterialSource() {
  }

  public LearningMaterialSource(
    String materialId,
    String ownerSubject,
    String sourceType,
    String sourceName,
    String originalFilename,
    String contentType,
    Long fileSize,
    String storageObjectKey,
    String storageObjectUri,
    String contentHash,
    String contentETag,
    byte[] content
  ) {
    this.materialId = materialId;
    this.ownerSubject = ownerSubject;
    this.sourceType = sourceType;
    this.sourceName = sourceName;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.fileSize = fileSize;
    this.storageObjectKey = storageObjectKey;
    this.storageObjectUri = storageObjectUri;
    this.contentHash = contentHash;
    this.contentETag = contentETag;
    this.content = content;
  }

  public static LearningMaterialSource create(
    String materialId,
    String ownerSubject,
    String sourceType,
    String sourceName,
    String originalFilename,
    String contentType,
    Long fileSize,
    String storageObjectKey,
    String storageObjectUri,
    String contentHash,
    String contentETag,
    byte[] content
  ) {
    return new LearningMaterialSource(
      materialId,
      ownerSubject,
      sourceType,
      sourceName,
      originalFilename,
      contentType,
      fileSize,
      storageObjectKey,
      storageObjectUri,
      contentHash,
      contentETag,
      content
    );
  }

  @PrePersist
  void prePersist() {
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
  }

  public void markDeleted() {
    deletedAt = Instant.now();
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

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceName() {
    return sourceName;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public String getStorageObjectKey() {
    return storageObjectKey;
  }

  public String getStorageObjectUri() {
    return storageObjectUri;
  }

  public String getContentHash() {
    return contentHash;
  }

  public String getContentETag() {
    return contentETag;
  }

  public byte[] getContent() {
    return content;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
