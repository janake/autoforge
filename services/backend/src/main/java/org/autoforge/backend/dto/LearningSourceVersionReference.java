package org.autoforge.backend.dto;

import java.time.Instant;

public record LearningSourceVersionReference(
  String id,
  String sourceName,
  String originalFilename,
  String contentHash,
  String contentETag,
  Instant createdAt
) {
}
