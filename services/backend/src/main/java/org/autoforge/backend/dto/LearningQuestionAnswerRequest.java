package org.autoforge.backend.dto;

public record LearningQuestionAnswerRequest(
  int questionIndex,
  int selectedOptionIndex
) {
}
