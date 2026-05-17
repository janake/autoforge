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
  String ownerSubject,
  List<String> studentSubjects,
  List<String> groupNames,
  boolean canManageAssignments,
  Instant createdAt,
  Instant updatedAt
) {
}
