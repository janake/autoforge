package org.autoforge.backend.dto;

import org.autoforge.backend.domain.PromptIntent;

public record ApprovePromptDraftRequest(
  PromptIntent selectedIntent
) {
}
