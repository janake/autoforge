package org.autoforge.backend.service;

import java.util.List;

public record PromptDraftClarificationResult(
  boolean readyForApproval,
  List<String> questions,
  String message
) {
  public PromptDraftClarificationResult {
    questions = questions == null ? List.of() : List.copyOf(questions);
  }
}
