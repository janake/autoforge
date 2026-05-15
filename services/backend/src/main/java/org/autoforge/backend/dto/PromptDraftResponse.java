package org.autoforge.backend.dto;

import java.time.Instant;
import java.util.List;

public record PromptDraftResponse(
  String draftId,
  String prompt,
  String status,
  boolean readyForApproval,
  List<String> pendingQuestions,
  List<PromptDraftMessageResponse> messages,
  Instant createdAt,
  Instant updatedAt
) {
}
