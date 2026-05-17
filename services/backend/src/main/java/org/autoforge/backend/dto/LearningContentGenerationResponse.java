package org.autoforge.backend.dto;

import java.time.Instant;
import java.util.List;
import org.autoforge.backend.domain.LearningContentGenerationType;

public record LearningContentGenerationResponse(
  String id,
  String materialId,
  LearningContentGenerationType generationType,
  String content,
  List<LearningContentSourceReference> sources,
  boolean fallbackUsed,
  String fallbackReason,
  Instant createdAt
) {
}
