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
@Table(name = "learning_image_assets")
public class LearningImageAsset {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "owner_subject", nullable = false, length = 128)
  private String ownerSubject;

  @Column(name = "storage_object_uri", nullable = false, length = 1000)
  private String storageObjectUri;

  @Column(name = "mime_type", nullable = false, length = 128)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "content_hash", nullable = false, length = 128)
  private String contentHash;

  @Column(name = "alt_text", length = 256)
  private String altText;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LearningImageAsset() {
  }

  public LearningImageAsset(String materialId, String ownerSubject, String storageObjectUri, String mimeType, long sizeBytes, String contentHash, String altText) {
    this.materialId = materialId;
    this.ownerSubject = ownerSubject;
    this.storageObjectUri = storageObjectUri;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.contentHash = contentHash;
    this.altText = altText;
  }

  public static LearningImageAsset create(String materialId, String ownerSubject, String storageObjectUri, String mimeType, long sizeBytes, String contentHash, String altText) {
    return new LearningImageAsset(materialId, ownerSubject, storageObjectUri, mimeType, sizeBytes, contentHash, altText);
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

  public String getStorageObjectUri() {
    return storageObjectUri;
  }

  public String getMimeType() {
    return mimeType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getContentHash() {
    return contentHash;
  }

  public String getAltText() {
    return altText;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
