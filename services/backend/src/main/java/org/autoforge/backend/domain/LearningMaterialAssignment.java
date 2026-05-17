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
@Table(name = "learning_material_assignments")
public class LearningMaterialAssignment {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 32)
  private LearningAssignmentTargetType targetType;

  @Column(name = "target_identifier", nullable = false, length = 128)
  private String targetIdentifier;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LearningMaterialAssignment() {
  }

  public LearningMaterialAssignment(String materialId, LearningAssignmentTargetType targetType, String targetIdentifier) {
    this.materialId = materialId;
    this.targetType = targetType;
    this.targetIdentifier = targetIdentifier;
  }

  public static LearningMaterialAssignment create(String materialId, LearningAssignmentTargetType targetType, String targetIdentifier) {
    return new LearningMaterialAssignment(materialId, targetType, targetIdentifier);
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

  public LearningAssignmentTargetType getTargetType() {
    return targetType;
  }

  public String getTargetIdentifier() {
    return targetIdentifier;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
