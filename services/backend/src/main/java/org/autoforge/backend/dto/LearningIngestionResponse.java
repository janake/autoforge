package org.autoforge.backend.dto;

import java.time.Instant;
import org.autoforge.backend.domain.LearningIngestionStatus;

public record LearningIngestionResponse(
  String jobId,
  String materialId,
  LearningIngestionStatus status,
  int retryCount,
  int chunkCount,
  int embeddingCount,
  String lastError,
  Instant createdAt,
  Instant updatedAt
) {
}
