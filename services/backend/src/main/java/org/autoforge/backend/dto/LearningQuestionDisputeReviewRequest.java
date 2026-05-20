package org.autoforge.backend.dto;

import org.autoforge.backend.domain.LearningQuestionDisputeStatus;

public record LearningQuestionDisputeReviewRequest(
  LearningQuestionDisputeStatus status,
  String reviewReason,
  Integer overrideScore
) {
}
