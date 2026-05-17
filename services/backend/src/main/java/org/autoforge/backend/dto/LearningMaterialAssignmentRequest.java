package org.autoforge.backend.dto;

import java.util.List;

public record LearningMaterialAssignmentRequest(
  List<String> studentSubjects,
  List<String> groupNames
) {
}
