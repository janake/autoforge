package org.autoforge.backend.service;

public class LearningMaterialNotFoundException extends RuntimeException {

  public LearningMaterialNotFoundException(String materialId) {
    super("Learning material not found: " + materialId);
  }
}
