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
@Table(name = "learning_assignment_audits")
public class LearningAssignmentAudit {

  @Id
  @Column(name = "audit_id", nullable = false, updatable = false, length = 36)
  private String auditId;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "actor_subject", nullable = false, length = 128)
  private String actorSubject;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 32)
  private LearningAssignmentAuditTargetType targetType;

  @Column(name = "target_identifier", nullable = false, length = 128)
  private String targetIdentifier;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 32)
  private LearningAssignmentAuditAction action;

  @Lob
  @Column(name = "details_json", nullable = false)
  private String detailsJson;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LearningAssignmentAudit() {
  }

  private LearningAssignmentAudit(String materialId, String actorSubject, LearningAssignmentAuditTargetType targetType, String targetIdentifier, LearningAssignmentAuditAction action, String detailsJson) {
    this.materialId = materialId;
    this.actorSubject = actorSubject;
    this.targetType = targetType;
    this.targetIdentifier = targetIdentifier;
    this.action = action;
    this.detailsJson = detailsJson;
  }

  public static LearningAssignmentAudit create(String materialId, String actorSubject, LearningAssignmentAuditTargetType targetType, String targetIdentifier, LearningAssignmentAuditAction action, String detailsJson) {
    return new LearningAssignmentAudit(materialId, actorSubject, targetType, targetIdentifier, action, detailsJson);
  }

  @PrePersist
  void prePersist() {
    if (auditId == null || auditId.isBlank()) {
      auditId = UUID.randomUUID().toString();
    }
  }

  public String getAuditId() {
    return auditId;
  }

  public String getMaterialId() {
    return materialId;
  }

  public String getActorSubject() {
    return actorSubject;
  }

  public LearningAssignmentAuditTargetType getTargetType() {
    return targetType;
  }

  public String getTargetIdentifier() {
    return targetIdentifier;
  }

  public LearningAssignmentAuditAction getAction() {
    return action;
  }

  public String getDetailsJson() {
    return detailsJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
