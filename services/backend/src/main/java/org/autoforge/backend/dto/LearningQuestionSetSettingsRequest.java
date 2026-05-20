package org.autoforge.backend.dto;

import java.time.Instant;

public record LearningQuestionSetSettingsRequest(
  Instant deadlineAt,
  Integer maxAttempts
) {
}
