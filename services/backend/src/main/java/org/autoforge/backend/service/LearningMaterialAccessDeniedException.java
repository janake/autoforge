package org.autoforge.backend.service;

public class LearningMaterialAccessDeniedException extends RuntimeException {

  public LearningMaterialAccessDeniedException(String materialId) {
    super("Learning material access denied: " + materialId);
  }
}
