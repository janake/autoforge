package org.autoforge.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
  @NotBlank String prompt
) {
}
