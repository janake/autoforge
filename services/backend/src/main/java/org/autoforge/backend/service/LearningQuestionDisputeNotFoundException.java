package org.autoforge.backend.service;

public class LearningQuestionDisputeNotFoundException extends RuntimeException {

  public LearningQuestionDisputeNotFoundException(String disputeId) {
    super("Learning question dispute not found: " + disputeId);
  }
}
