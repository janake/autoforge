package org.autoforge.backend.dto;

public record LearningQuestionDisputeRequest(
  int questionIndex,
  int selectedOptionIndex,
  String reason
) {
}
