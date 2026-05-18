package org.autoforge.backend.dto;

import java.util.List;

public record LearningQuestionAttemptRequest(
  String generationId,
  List<LearningQuestionAnswerRequest> answers
) {
}
