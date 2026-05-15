package org.autoforge.backend.service;

public class PromptDraftNotFoundException extends RuntimeException {

  public PromptDraftNotFoundException(String draftId) {
    super("Prompt draft not found: " + draftId);
  }
}
