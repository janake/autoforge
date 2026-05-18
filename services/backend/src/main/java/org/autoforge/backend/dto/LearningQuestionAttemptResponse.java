package org.autoforge.backend.dto;

import java.time.Instant;

public record LearningQuestionAttemptResponse(
  String id,
  String materialId,
  String generationId,
  String studentSubject,
  int score,
  int totalQuestions,
  String answers,
  Instant submittedAt
) {
}
