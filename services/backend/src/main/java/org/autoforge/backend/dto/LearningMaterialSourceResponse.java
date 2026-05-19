package org.autoforge.backend.dto;

import java.time.Instant;

public record LearningMaterialSourceResponse(
  String id,
  String materialId,
  String sourceType,
  String sourceName,
  String originalFilename,
  String contentType,
  Long fileSize,
  String storageObjectKey,
  String storageObjectUri,
  String contentHash,
  String contentETag,
  Instant deletedAt,
  Instant createdAt,
  Instant updatedAt
) {
}
