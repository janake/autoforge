package org.autoforge.backend.service;

import java.util.List;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.dto.LearningContentSourceReference;

public interface LearningAiEngineProvider {

  LearningGenerationResult generate(GenerationContext context);

  record GenerationContext(
    String materialId,
    String title,
    String description,
    String materialText,
    String optimizedImageUrl,
    String retrievalContext,
    LearningContentGenerationType generationType
  ) {
  }

  record LearningGenerationResult(
    String content,
    String structuredContent,
    List<LearningContentSourceReference> sources,
    boolean fallbackUsed,
    String fallbackReason
  ) {
  }
}
