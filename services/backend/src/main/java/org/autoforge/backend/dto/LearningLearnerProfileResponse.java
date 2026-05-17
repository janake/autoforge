package org.autoforge.backend.dto;

import java.time.Instant;

public record LearningLearnerProfileResponse(
  String ownerSubject,
  String knowledgeLevel,
  String preferredQuestionStyle,
  String preferredExplanationStyle,
  String studyGoal,
  String promptNotes,
  String retrievalContext,
  Instant createdAt,
  Instant updatedAt
) {
}
