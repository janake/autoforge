package org.autoforge.backend.dto;

import java.util.List;

public record LearningQuestionPayload(
  String prompt,
  List<LearningQuestionOptionPayload> options,
  int correctOptionIndex,
  String explanation,
  List<LearningContentSourceReference> sources,
  String imageAssetReference
) {
}
