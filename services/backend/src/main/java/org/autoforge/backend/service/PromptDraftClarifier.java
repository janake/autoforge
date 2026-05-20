package org.autoforge.backend.service;

public interface PromptDraftClarifier {
  PromptDraftClarificationResult clarify(PromptDraftClarificationRequest request);
}
