package org.autoforge.backend.service;

public class LearningIngestionNotFoundException extends RuntimeException {

  public LearningIngestionNotFoundException(String materialId) {
    super("Learning ingestion not found for material: " + materialId);
  }
}
