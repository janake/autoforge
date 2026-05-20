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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.autoforge.backend.domain.LearningChunk;
import org.autoforge.backend.domain.LearningEmbedding;
import org.autoforge.backend.domain.LearningIngestionJob;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.dto.LearningEmbeddingPayload;
import org.autoforge.backend.repository.LearningChunkRepository;
import org.autoforge.backend.repository.LearningEmbeddingRepository;
import org.autoforge.backend.repository.LearningIngestionJobRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningMaterialSourceRepository;
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
  private LearningMaterialSourceRepository learningMaterialSourceRepository;

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
    learningMaterialSourceRepository.deleteAll();
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
    LearningIngestionJob job = learningIngestionJobRepository.findByMaterialId(material.getId()).orElseThrow();
    assertThat(learningChunkRepository.count()).isEqualTo(2);
    assertThat(learningEmbeddingRepository.count()).isEqualTo(2);

    List<LearningChunk> chunks = learningChunkRepository.findByJobIdOrderByChunkIndexAsc(job.getId());
    assertThat(chunks).hasSize(2);
    assertThat(chunks.get(0).getOwnerSubject()).isEqualTo("teacher-1");
    assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
    assertThat(chunks.get(0).getSourceStartOffset()).isEqualTo(0);
    assertThat(chunks.get(0).getSourceEndOffset()).isGreaterThan(chunks.get(0).getSourceStartOffset());
    assertThat(chunks.get(1).getSourceStartOffset()).isGreaterThan(chunks.get(0).getSourceStartOffset());

    LearningEmbedding firstEmbedding = learningEmbeddingRepository.findAll().stream()
      .filter(embedding -> embedding.getChunkId().equals(chunks.get(0).getId()))
      .findFirst()
      .orElseThrow();
    assertThat(firstEmbedding.getOwnerSubject()).isEqualTo("teacher-1");
    assertThat(firstEmbedding.getModel()).isEqualTo("mock-embed");
    assertThat(firstEmbedding.getDimensions()).isEqualTo(4);
    assertThat(firstEmbedding.getVectorJson()).contains("0.1");

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.retryCount").value(0));
  }

  @Test
  void startIngestionUsesActiveSourcesBeforeMaterialBody() throws Exception {
    when(learningEmbeddingProvider.embed(anyString())).thenAnswer(invocation -> new LearningEmbeddingPayload("mock-embed", 4, "[0.1, 0.2, 0.3, 0.4]"));

    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.create(
      "teacher-1",
      "Több forrás",
      "source ingestion"
    ));
    learningMaterialSourceRepository.save(org.autoforge.backend.domain.LearningMaterialSource.create(
      material.getId(),
      "teacher-1",
      "PRIMARY_UPLOAD",
      "Első forrás",
      "first.txt",
      "text/plain",
      18L,
      "teacher-1/source-1",
      "oci://learning-materials/teacher-1/source-1",
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "0123456789abcdef0123456789abcdef",
      "Első bekezdés".getBytes(StandardCharsets.UTF_8)
    ));
    learningMaterialSourceRepository.save(org.autoforge.backend.domain.LearningMaterialSource.create(
      material.getId(),
      "teacher-1",
      "ADDITIONAL_UPLOAD",
      "Második forrás",
      "second.txt",
      "text/plain",
      19L,
      "teacher-1/source-2",
      "oci://learning-materials/teacher-1/source-2",
      "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
      "abcdef0123456789abcdef0123456789",
      "Második bekezdés".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/ingestion", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.chunkCount").value(2));

    List<LearningChunk> chunks = learningChunkRepository.findAll();
    assertThat(chunks).hasSize(2);
    assertThat(chunks.get(0).getContent()).contains("Első bekezdés");
    assertThat(chunks.get(1).getContent()).contains("Második bekezdés");
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
