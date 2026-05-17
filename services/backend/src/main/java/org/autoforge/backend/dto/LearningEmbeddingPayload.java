package org.autoforge.backend.dto;

public record LearningEmbeddingPayload(String model, int dimensions, String vectorJson) {
}
