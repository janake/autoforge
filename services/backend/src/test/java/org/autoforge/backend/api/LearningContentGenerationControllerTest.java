package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.repository.LearningGeneratedContentRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningQuestionAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

  @Autowired
  private LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;

  @Autowired
  private LearningQuestionAttemptRepository learningQuestionAttemptRepository;

  @BeforeEach
  void cleanState() {
    learningQuestionAttemptRepository.deleteAll();
    learningMaterialAssignmentRepository.deleteAll();
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
      .andExpect(jsonPath("$.questionSetStatus").value("DRAFT"))
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
  void assignedStudentSeesOnlyPublishedQuestionSets() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Biológia",
      "Gyakorló tananyag",
      "biology.txt",
      "text/plain",
      32L,
      "Sejtek és szövetek alapjai.\n\nMásodik bekezdés a részletekről.".getBytes(StandardCharsets.UTF_8)
    ));
    learningMaterialAssignmentRepository.save(LearningMaterialAssignment.create(material.getId(), LearningAssignmentTargetType.STUDENT, "student-1"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/questions", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated());

    LearningGeneratedContent questionSet = learningGeneratedContentRepository.findAll().stream()
      .filter(content -> content.getGenerationType() == LearningContentGenerationType.QUESTION_SET)
      .findFirst()
      .orElseThrow();

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-sets", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/publish", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questionSetStatus").value("PUBLISHED"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-sets", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(questionSet.getId()))
      .andExpect(jsonPath("$[0].questionSetStatus").value("PUBLISHED"));
  }

  @Test
  void archivedQuestionSetCannotBeUsedForNewAttempts() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Kémia",
      "Gyakorló tananyag",
      "chemistry.txt",
      "text/plain",
      28L,
      "Atomok és molekulák.\n\nMásodik bekezdés.".getBytes(StandardCharsets.UTF_8)
    ));
    learningMaterialAssignmentRepository.save(LearningMaterialAssignment.create(material.getId(), LearningAssignmentTargetType.STUDENT, "student-1"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/questions", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated());

    LearningGeneratedContent questionSet = learningGeneratedContentRepository.findAll().stream()
      .filter(content -> content.getGenerationType() == LearningContentGenerationType.QUESTION_SET)
      .findFirst()
      .orElseThrow();

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/publish", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questionSetStatus").value("PUBLISHED"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/archive", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questionSetStatus").value("ARCHIVED"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-sets", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndex": 0}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isForbidden());
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

  @Test
  void assignedStudentCanSubmitQuestionAttemptAndOwnerCanSeeResults() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Biológia",
      "Gyakorló tananyag",
      "biology.txt",
      "text/plain",
      32L,
      "Sejtek és szövetek alapjai.".getBytes(StandardCharsets.UTF_8)
    ));
    learningMaterialAssignmentRepository.save(LearningMaterialAssignment.create(material.getId(), LearningAssignmentTargetType.STUDENT, "student-1"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/questions", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated());

    LearningGeneratedContent questionSet = learningGeneratedContentRepository.findAll().get(0);

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/publish", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questionSetStatus").value("PUBLISHED"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-sets", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(questionSet.getId()))
      .andExpect(jsonPath("$[0].structuredContent").value(org.hamcrest.Matchers.containsString("questions")));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "generationId": "%s",
            "answers": [
              {"questionIndex": 0, "selectedOptionIndex": 0},
              {"questionIndex": 1, "selectedOptionIndex": 1}
            ]
          }
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.materialId").value(material.getId()))
      .andExpect(jsonPath("$.studentSubject").value("student-1"))
      .andExpect(jsonPath("$.score").value(1))
      .andExpect(jsonPath("$.totalQuestions").value(3))
      .andExpect(jsonPath("$.answers").value(org.hamcrest.Matchers.containsString("selectedOptionIndex")));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].studentSubject").value("student-1"));
  }

  @Test
  void studentCannotReadOtherStudentsQuestionAttempts() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Kémia",
      "Gyakorló tananyag",
      "chemistry.txt",
      "text/plain",
      28L,
      "Atomok és molekulák.".getBytes(StandardCharsets.UTF_8)
    ));
    learningMaterialAssignmentRepository.save(LearningMaterialAssignment.create(material.getId(), LearningAssignmentTargetType.STUDENT, "student-1"));
    learningMaterialAssignmentRepository.save(LearningMaterialAssignment.create(material.getId(), LearningAssignmentTargetType.STUDENT, "student-2"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/questions", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated());

    LearningGeneratedContent questionSet = learningGeneratedContentRepository.findAll().get(0);

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/publish", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questionSetStatus").value("PUBLISHED"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndex": 0}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated());

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-2"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0]").doesNotExist());
  }
}
