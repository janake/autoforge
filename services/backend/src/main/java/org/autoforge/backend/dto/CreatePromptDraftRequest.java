package org.autoforge.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePromptDraftRequest(
  @NotBlank String prompt
) {
}
