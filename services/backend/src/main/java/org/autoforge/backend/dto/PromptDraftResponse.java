package org.autoforge.backend.dto;

import java.time.Instant;
import java.util.List;

public record PromptDraftResponse(
  String draftId,
  String prompt,
  String status,
  String intent,
  Double intentConfidence,
  String intentReason,
  String selectedIntent,
  boolean readyForApproval,
  String approvedBy,
  Instant approvedAt,
  String jiraIssueKey,
  String jiraIssueUrl,
  List<String> pendingQuestions,
  List<PromptDraftMessageResponse> messages,
  Instant createdAt,
  Instant updatedAt
) {
}
