package org.autoforge.backend.dto;

import java.time.Instant;

public record PromptDraftMessageResponse(
  String role,
  String content,
  Instant createdAt
) {
}
