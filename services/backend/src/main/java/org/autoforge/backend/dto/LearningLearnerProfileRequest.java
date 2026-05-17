package org.autoforge.backend.dto;

public record LearningLearnerProfileRequest(
  String knowledgeLevel,
  String preferredQuestionStyle,
  String preferredExplanationStyle,
  String studyGoal,
  String promptNotes
) {
}
