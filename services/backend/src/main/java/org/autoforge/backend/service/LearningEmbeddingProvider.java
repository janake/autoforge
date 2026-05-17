package org.autoforge.backend.service;

import org.autoforge.backend.dto.LearningEmbeddingPayload;

public interface LearningEmbeddingProvider {

  LearningEmbeddingPayload embed(String text);
}
