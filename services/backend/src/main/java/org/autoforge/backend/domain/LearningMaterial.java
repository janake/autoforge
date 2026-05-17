package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

  public static LearningMaterial create(String ownerSubject, String title, String description) {
    return new LearningMaterial(ownerSubject, title, description);
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
