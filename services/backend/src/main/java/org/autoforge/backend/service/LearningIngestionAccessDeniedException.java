package org.autoforge.backend.service;

public class LearningIngestionAccessDeniedException extends RuntimeException {

  public LearningIngestionAccessDeniedException(String materialId) {
    super("Learning ingestion access denied for material: " + materialId);
  }
}
