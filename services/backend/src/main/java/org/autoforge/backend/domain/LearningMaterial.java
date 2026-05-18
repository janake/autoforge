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
@Table(name = "learning_materials")
public class LearningMaterial {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Column(name = "title", nullable = false, length = 256)
  private String title;

  @Column(name = "description", length = 4000)
  private String description;

  @Column(name = "original_filename", length = 256)
  private String originalFilename;

  @Column(name = "content_type", length = 128)
  private String contentType;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "storage_object_key", length = 512)
  private String storageObjectKey;

  @Column(name = "storage_object_uri", length = 1000)
  private String storageObjectUri;

  @Column(name = "content_hash", length = 128)
  private String contentHash;

  @Column(name = "content_etag", length = 64)
  private String contentETag;

  @Lob
  @Column(name = "content")
  private byte[] content;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LearningMaterial() {
  }

  public LearningMaterial(String ownerSubject, String title, String description) {
    this.ownerSubject = ownerSubject;
    this.title = title;
    this.description = description;
  }

  public LearningMaterial(String ownerSubject, String title, String description, String originalFilename, String contentType, Long fileSize, byte[] content) {
    this.ownerSubject = ownerSubject;
    this.title = title;
    this.description = description;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.fileSize = fileSize;
    this.content = content;
  }

  public LearningMaterial(
    String ownerSubject,
    String title,
    String description,
    String originalFilename,
    String contentType,
    Long fileSize,
    String storageObjectKey,
    String storageObjectUri,
    String contentHash,
    String contentETag,
    byte[] content
  ) {
    this(ownerSubject, title, description, originalFilename, contentType, fileSize, content);
    this.storageObjectKey = storageObjectKey;
    this.storageObjectUri = storageObjectUri;
    this.contentHash = contentHash;
    this.contentETag = contentETag;
  }

  public static LearningMaterial create(String ownerSubject, String title, String description) {
    return new LearningMaterial(ownerSubject, title, description);
  }

  public static LearningMaterial createUploaded(
    String ownerSubject,
    String title,
    String description,
    String originalFilename,
    String contentType,
    Long fileSize,
    byte[] content
  ) {
    return new LearningMaterial(ownerSubject, title, description, originalFilename, contentType, fileSize, content);
  }

  public static LearningMaterial createUploaded(
    String ownerSubject,
    String title,
    String description,
    String originalFilename,
    String contentType,
    Long fileSize,
    String storageObjectKey,
    String storageObjectUri,
    String contentHash,
    String contentETag,
    byte[] content
  ) {
    return new LearningMaterial(
      ownerSubject,
      title,
      description,
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

  public String getId() {
    return id;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
