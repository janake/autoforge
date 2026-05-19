package org.autoforge.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningMaterialGenerationHistoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningMaterialRepository learningMaterialRepository;

  @BeforeEach
  void cleanState() {
    learningMaterialRepository.deleteAll();
  }

  @Test
  void ownerCanListGeneratedQuestionsAndSummaries() throws Exception {
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
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/summary", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated());

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/generations", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].generationType").value("SUMMARY"))
      .andExpect(jsonPath("$[1].generationType").value("QUESTION_SET"))
      .andExpect(jsonPath("$[1].questionSetStatus").value("DRAFT"));
  }

  @Test
  void nonOwnerCannotListGeneratedContent() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Fizika",
      "Owner only",
      "physics.txt",
      "text/plain",
      24L,
      "Egy rövid tananyag.".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/generations", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2"))))
      .andExpect(status().isForbidden());
  }
}
