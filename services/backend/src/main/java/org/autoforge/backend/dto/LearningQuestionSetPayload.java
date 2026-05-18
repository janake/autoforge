package org.autoforge.backend.dto;

import java.util.List;

public record LearningQuestionSetPayload(
  String materialId,
  String materialTitle,
  String retrievalContext,
  List<LearningQuestionPayload> questions
) {
}
