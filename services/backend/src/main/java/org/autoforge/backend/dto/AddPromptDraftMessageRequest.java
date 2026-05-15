package org.autoforge.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AddPromptDraftMessageRequest(
  @NotBlank String content
) {
}
