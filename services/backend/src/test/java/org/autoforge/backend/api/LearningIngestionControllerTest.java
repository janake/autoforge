package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.dto.LearningEmbeddingPayload;
import org.autoforge.backend.repository.LearningChunkRepository;
import org.autoforge.backend.repository.LearningEmbeddingRepository;
import org.autoforge.backend.repository.LearningIngestionJobRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.service.LearningEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningIngestionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningMaterialRepository learningMaterialRepository;

  @Autowired
  private LearningIngestionJobRepository learningIngestionJobRepository;

  @Autowired
  private LearningChunkRepository learningChunkRepository;

  @Autowired
  private LearningEmbeddingRepository learningEmbeddingRepository;

  @MockBean
  private LearningEmbeddingProvider learningEmbeddingProvider;

  @BeforeEach
  void cleanState() {
    learningEmbeddingRepository.deleteAll();
    learningChunkRepository.deleteAll();
    learningIngestionJobRepository.deleteAll();
    learningMaterialRepository.deleteAll();
  }

  @Test
  void startIngestionCreatesChunksAndEmbeddings() throws Exception {
    when(learningEmbeddingProvider.embed(anyString())).thenAnswer(invocation -> new LearningEmbeddingPayload("mock-embed", 4, "[0.1, 0.2, 0.3, 0.4]"));

    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Algebra alapok",
      "Bevezető tananyag",
      "algebra.md",
      "text/markdown",
      56L,
      "Első bekezdés\n\nMásodik bekezdés".getBytes(StandardCharsets.UTF_8)
    ));

    String response = mockMvc.perform(post("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
      .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.materialId").value(material.getId()))
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.chunkCount").value(2))
      .andExpect(jsonPath("$.embeddingCount").value(2))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertThat(response).contains(material.getId());
    assertThat(learningIngestionJobRepository.findByMaterialId(material.getId())).isPresent();
    assertThat(learningChunkRepository.count()).isEqualTo(2);
    assertThat(learningEmbeddingRepository.count()).isEqualTo(2);

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.retryCount").value(0));
  }

  @Test
  void retryIngestionRecoversAfterProviderFailure() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    when(learningEmbeddingProvider.embed(anyString())).thenAnswer(invocation -> {
      if (attempts.incrementAndGet() == 1) {
        throw new IllegalStateException("embedding provider unavailable");
      }
      return new LearningEmbeddingPayload("mock-embed", 4, "[0.1, 0.2, 0.3, 0.4]");
    });

    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Geometria",
      "Retry tananyag",
      "geometry.txt",
      "text/plain",
      40L,
      "Csak egy rövid szöveg".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("FAILED"))
      .andExpect(jsonPath("$.retryCount").value(0));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/ingestion/retry", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.retryCount").value(1))
      .andExpect(jsonPath("$.embeddingCount").value(1));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.retryCount").value(1));
  }

  @Test
  void nonOwnerCannotSeeIngestionStatus() throws Exception {
    when(learningEmbeddingProvider.embed(anyString())).thenReturn(new LearningEmbeddingPayload("mock-embed", 4, "[0.1, 0.2, 0.3, 0.4]"));

    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Fizika",
      "Access test",
      "physics.txt",
      "text/plain",
      28L,
      "tananyag".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2"))))
      .andExpect(status().isForbidden());
  }
}
