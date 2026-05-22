package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningMaterialSource;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.repository.LearningGeneratedContentRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningMaterialSourceRepository;
import org.autoforge.backend.repository.LearningQuestionAttemptRepository;
import org.autoforge.backend.repository.LearningQuestionProgressRepository;
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
  private LearningMaterialSourceRepository learningMaterialSourceRepository;

  @Autowired
  private LearningGeneratedContentRepository learningGeneratedContentRepository;

  @Autowired
  private LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;

  @Autowired
  private LearningQuestionAttemptRepository learningQuestionAttemptRepository;

  @Autowired
  private LearningQuestionProgressRepository learningQuestionProgressRepository;

  @BeforeEach
  void cleanState() {
    learningQuestionAttemptRepository.deleteAll();
    learningQuestionProgressRepository.deleteAll();
    learningMaterialAssignmentRepository.deleteAll();
    learningMaterialSourceRepository.deleteAll();
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
    learningMaterialSourceRepository.save(LearningMaterialSource.create(
      material.getId(),
      "teacher-1",
      "PRIMARY_UPLOAD",
      "Algebra alapok",
      "algebra.txt",
      "text/plain",
      68L,
      "teacher-1/source-1",
      "oci://learning-materials/teacher-1/source-1",
      "1111111111111111111111111111111111111111111111111111111111111111",
      "11111111111111111111111111111111",
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
      .andExpect(jsonPath("$.sourceVersions[0].contentHash").value("1111111111111111111111111111111111111111111111111111111111111111"))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.containsString("\"answerType\":\"SINGLE_CORRECT\"")))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.containsString("\"answerType\":\"MULTI_CORRECT\"")))
      .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Tanulói profil:")))
      .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Mi a legfontosabb üzenete a tananyagnak?")))
      .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Válassz 2 választ")))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.containsString("\"questions\"")))
      .andExpect(jsonPath("$.structuredContent").value(org.hamcrest.Matchers.containsString("\"options\"")))
      .andExpect(jsonPath("$.sources[0].chunkIndex").value(0));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/summary", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.generationType").value(LearningContentGenerationType.SUMMARY.name()))
      .andExpect(jsonPath("$.generationStatus").value("COMPLETED"))
      .andExpect(jsonPath("$.sourceVersions[0].sourceName").value("Algebra alapok"))
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

    LearningMaterialSource source = learningMaterialSourceRepository.findByMaterialIdAndDeletedAtIsNullOrderByCreatedAtAsc(material.getId()).get(0);
    source.markDeleted();
    learningMaterialSourceRepository.save(source);

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/summary", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.sourceVersions").isEmpty());

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/generations", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].sourceVersions").isEmpty())
      .andExpect(jsonPath("$[1].sourceVersions[0].contentHash").value("1111111111111111111111111111111111111111111111111111111111111111"));
  }

  @Test
  void assignedStudentSeesOnlyPublishedQuestionSetsAfterPublish() throws Exception {
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
  void archivedQuestionSetCannotBeUsedForNewAttemptsAfterArchive() throws Exception {
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
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [0]}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isForbidden());
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
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [0]}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isForbidden());
  }

  @Test
  void questionSetSettingsPersistAndLimitAttempts() throws Exception {
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

    LearningGeneratedContent questionSet = learningGeneratedContentRepository.findAll().stream()
      .filter(content -> content.getGenerationType() == LearningContentGenerationType.QUESTION_SET)
      .findFirst()
      .orElseThrow();

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/publish", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questionSetStatus").value("PUBLISHED"));

    mockMvc.perform(put("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/settings", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"deadlineAt": "2099-05-21T12:00:00Z", "maxAttempts": 1}
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.deadlineAt").value("2099-05-21T12:00:00Z"))
      .andExpect(jsonPath("$.maxAttempts").value(1));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-sets", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].deadlineAt").value("2099-05-21T12:00:00Z"))
      .andExpect(jsonPath("$[0].maxAttempts").value(1));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [0]}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [0]}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isForbidden());
  }

  @Test
  void pastDeadlineBlocksNewAttempts() throws Exception {
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

    mockMvc.perform(put("/api/v1/learning/materials/{materialId}/question-sets/{generationId}/settings", material.getId(), questionSet.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"deadlineAt": "2026-01-01T00:00:00Z", "maxAttempts": null}
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.deadlineAt").value("2026-01-01T00:00:00Z"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [0]}]}
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
              {"questionIndex": 0, "selectedOptionIndexes": [0]},
              {"questionIndex": 1, "selectedOptionIndexes": [0, 2]},
              {"questionIndex": 2, "selectedOptionIndexes": [0]}
            ]
          }
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.materialId").value(material.getId()))
      .andExpect(jsonPath("$.studentSubject").value("student-1"))
      .andExpect(jsonPath("$.score").value(3))
      .andExpect(jsonPath("$.totalQuestions").value(3))
      .andExpect(jsonPath("$.answers").value(org.hamcrest.Matchers.containsString("selectedOptionIndexes")));

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
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [0]}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated());

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-2"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0]").doesNotExist());
  }
}
