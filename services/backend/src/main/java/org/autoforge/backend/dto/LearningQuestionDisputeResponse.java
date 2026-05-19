package org.autoforge.backend.dto;

import java.time.Instant;
import org.autoforge.backend.domain.LearningQuestionDisputeStatus;

public record LearningQuestionDisputeResponse(
  String id,
  String materialId,
  String attemptId,
  String studentSubject,
  int questionIndex,
  int selectedOptionIndex,
  String reason,
  LearningQuestionDisputeStatus status,
  String reviewerSubject,
  String reviewReason,
  Integer overrideScore,
  Instant createdAt,
  Instant reviewedAt
) {
}
