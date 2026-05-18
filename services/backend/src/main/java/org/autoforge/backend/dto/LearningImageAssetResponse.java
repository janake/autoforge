package org.autoforge.backend.dto;

import java.time.Instant;

public record LearningImageAssetResponse(
  String id,
  String materialId,
  String mimeType,
  long sizeBytes,
  String contentHash,
  String altText,
  String assetUrl,
  Instant createdAt
) {
}
