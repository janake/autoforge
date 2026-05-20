package org.autoforge.backend.dto;

import java.time.Instant;
import java.util.List;
import org.autoforge.backend.domain.LearningQuestionProgressStatus;

public record LearningQuestionProgressResponse(
  String id,
  String materialId,
  String generationId,
  String studentSubject,
  List<String> studentGroups,
  LearningQuestionProgressStatus status,
  String attemptId,
  int attemptCount,
  Integer score,
  Integer totalQuestions,
  Instant createdAt,
  Instant updatedAt,
  Instant startedAt,
  Instant submittedAt,
  Instant reviewedAt,
  Instant completedAt
) {
}
