package org.autoforge.backend.service;

import org.autoforge.backend.domain.PromptIntent;

public record PromptIntentClassification(
  PromptIntent intent,
  double confidence,
  String reason
) {
}
