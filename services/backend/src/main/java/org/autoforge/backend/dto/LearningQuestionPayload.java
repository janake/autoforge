package org.autoforge.backend.dto;

import java.util.List;
import org.autoforge.backend.domain.LearningQuestionAnswerType;

public record LearningQuestionPayload(
  String prompt,
  List<LearningQuestionOptionPayload> options,
  Integer correctOptionIndex,
  LearningQuestionAnswerType answerType,
  List<Integer> correctOptionIndexes,
  String explanation,
  List<LearningContentSourceReference> sources,
  String imageAssetReference
) {

  public List<Integer> resolvedCorrectOptionIndexes() {
    if (correctOptionIndexes != null && !correctOptionIndexes.isEmpty()) {
      return correctOptionIndexes;
    }
    if (correctOptionIndex != null) {
      return List.of(correctOptionIndex);
    }
    return List.of();
  }

  public LearningQuestionAnswerType resolvedAnswerType() {
    if (answerType != null) {
      return answerType;
    }
    return resolvedCorrectOptionIndexes().size() > 1
      ? LearningQuestionAnswerType.MULTI_CORRECT
      : LearningQuestionAnswerType.SINGLE_CORRECT;
  }
}
