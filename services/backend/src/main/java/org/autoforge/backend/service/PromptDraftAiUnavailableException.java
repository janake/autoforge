package org.autoforge.backend.service;

public class PromptDraftAiUnavailableException extends RuntimeException {
  public PromptDraftAiUnavailableException(String message) {
    super(message);
  }

  public PromptDraftAiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
