package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.repository.LearningGeneratedContentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningContentGenerationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningMaterialRepository learningMaterialRepository;

  @Autowired
  private LearningGeneratedContentRepository learningGeneratedContentRepository;

  @BeforeEach
  void cleanState() {
    learningGeneratedContentRepository.deleteAll();
    learningMaterialRepository.deleteAll();
  }

  @Test
  void ownerCanGenerateQuestionsAndSummaryFromMaterial() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Algebra alapok",
      "Bevezető tananyag",
      "algebra.txt",
      "text/plain",
      68L,
      "Első bekezdés az anyagról.\n\nMásodik bekezdés a részletekről.".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/questions", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.materialId").value(material.getId()))
      .andExpect(jsonPath("$.generationType").value(LearningContentGenerationType.QUESTION_SET.name()))
      .andExpect(jsonPath("$.generationStatus").value("COMPLETED"))
      .andExpect(jsonPath("$.fallbackUsed").value(true))
      .andExpect(jsonPath("$.fallbackReason").value("Real AI is unavailable because the learning generation provider is not configured."))
      .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Tanulói profil:")))
      .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Mi a legfontosabb üzenete a tananyagnak?")))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.containsString("\"questions\"")))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.containsString("\"options\"")))
      .andExpect(jsonPath("$.sources[0].chunkIndex").value(0));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/summary", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.generationType").value(LearningContentGenerationType.SUMMARY.name()))
      .andExpect(jsonPath("$.generationStatus").value("COMPLETED"))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.nullValue()))
      .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Chunk 1")))
      .andExpect(jsonPath("$.sources[0].excerpt").exists());

    assertThat(learningGeneratedContentRepository.findAll())
      .extracting(LearningGeneratedContent::getGenerationType)
      .containsExactlyInAnyOrder(LearningContentGenerationType.QUESTION_SET, LearningContentGenerationType.SUMMARY);

    LearningGeneratedContent questionSet = learningGeneratedContentRepository.findAll().stream()
      .filter(content -> content.getGenerationType() == LearningContentGenerationType.QUESTION_SET)
      .findFirst()
      .orElseThrow();
    assertThat(questionSet.getStructuredContent()).contains("\"questions\"");
    assertThat(questionSet.getGenerationStatus().name()).isEqualTo("COMPLETED");
  }

  @Test
  void nonOwnerCannotGenerateLearningContent() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Fizika",
      "Owner only",
      "physics.txt",
      "text/plain",
      24L,
      "Egy rövid tananyag.".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/questions", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2"))))
      .andExpect(status().isForbidden());

    assertThat(learningGeneratedContentRepository.findAll()).isEmpty();
  }
}
