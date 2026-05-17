package org.autoforge.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.autoforge.backend.dto.LearningEmbeddingPayload;
import org.springframework.stereotype.Component;

@Component
public class DeterministicLearningEmbeddingProvider implements LearningEmbeddingProvider {

  private static final String MODEL = "autoforge-learning-embed-v1";
  private static final int DIMENSIONS = 8;

  @Override
  public LearningEmbeddingPayload embed(String text) {
    byte[] digest = digest(text == null ? "" : text);
    double[] vector = new double[DIMENSIONS];

    for (int index = 0; index < DIMENSIONS; index++) {
      int value = Byte.toUnsignedInt(digest[index % digest.length]);
      vector[index] = value / 255.0d;
    }

    return new LearningEmbeddingPayload(MODEL, DIMENSIONS, Arrays.toString(vector));
  }

  private byte[] digest(String text) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Missing SHA-256 algorithm", exception);
    }
  }
}
