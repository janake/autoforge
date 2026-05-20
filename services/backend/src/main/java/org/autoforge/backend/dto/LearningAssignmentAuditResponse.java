package org.autoforge.backend.dto;

import java.time.Instant;
import org.autoforge.backend.domain.LearningAssignmentAuditAction;
import org.autoforge.backend.domain.LearningAssignmentAuditTargetType;

public record LearningAssignmentAuditResponse(
  String id,
  String materialId,
  String actorSubject,
  LearningAssignmentAuditTargetType targetType,
  String targetIdentifier,
  LearningAssignmentAuditAction action,
  String detailsJson,
  Instant createdAt
) {
}
