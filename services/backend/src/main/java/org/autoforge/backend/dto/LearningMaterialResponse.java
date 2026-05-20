package org.autoforge.backend.dto;

import java.time.Instant;
import java.util.List;

public record LearningMaterialResponse(
  String id,
  String title,
  String description,
  String originalFilename,
  String contentType,
  Long fileSize,
  String storageObjectKey,
  String storageObjectUri,
  String contentHash,
  String contentETag,
  String ownerSubject,
  List<String> studentSubjects,
  List<String> groupNames,
  boolean canManageAssignments,
  List<LearningMaterialSourceResponse> sources,
  List<LearningImageAssetResponse> imageAssets,
  List<LearningQuestionProgressResponse> progressEntries,
  String optimizedImageUrl,
  Long optimizedFileSize,
  String optimizedContentHash,
  Instant createdAt,
  Instant updatedAt
) {
}
