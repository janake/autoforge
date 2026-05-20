package org.autoforge.backend.dto;

import java.time.Instant;
import java.util.List;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGenerationStatus;
import org.autoforge.backend.domain.LearningQuestionSetStatus;

public record LearningContentGenerationResponse(
  String id,
  String materialId,
  LearningContentGenerationType generationType,
  String content,
  List<LearningContentSourceReference> sources,
  List<LearningSourceVersionReference> sourceVersions,
  LearningQuestionSetStatus questionSetStatus,
  boolean fallbackUsed,
  String fallbackReason,
  LearningGenerationStatus generationStatus,
  String structuredContent,
  String errorMessage,
  Instant createdAt
) {
}
