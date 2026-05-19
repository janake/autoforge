package org.autoforge.backend.dto;

import java.util.List;

public record LearningQuestionAnswerRequest(
  int questionIndex,
  List<Integer> selectedOptionIndexes
) {
}
