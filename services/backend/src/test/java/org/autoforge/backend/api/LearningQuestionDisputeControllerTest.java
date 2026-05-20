package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.repository.LearningGeneratedContentRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningQuestionAttemptRepository;
import org.autoforge.backend.repository.LearningQuestionDisputeRepository;
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
class LearningQuestionDisputeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningMaterialRepository learningMaterialRepository;

  @Autowired
  private LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;

  @Autowired
  private LearningGeneratedContentRepository learningGeneratedContentRepository;

  @Autowired
  private LearningQuestionAttemptRepository learningQuestionAttemptRepository;

  @Autowired
  private LearningQuestionDisputeRepository learningQuestionDisputeRepository;

  @Autowired
  private LearningQuestionProgressRepository learningQuestionProgressRepository;

  @BeforeEach
  void cleanState() {
    learningQuestionDisputeRepository.deleteAll();
    learningQuestionAttemptRepository.deleteAll();
    learningQuestionProgressRepository.deleteAll();
    learningGeneratedContentRepository.deleteAll();
    learningMaterialAssignmentRepository.deleteAll();
    learningMaterialRepository.deleteAll();
  }

  @Test
  void studentCanOpenDisputeAndTeacherCanReviewWithScoreOverride() throws Exception {
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
      .andExpect(jsonPath("$[0].id").value(questionSet.getId()));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.progressEntries[0].status").value("STARTED"));

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "generationId": "%s",
            "answers": [{"questionIndex": 0, "selectedOptionIndexes": [1]}]
          }
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.score").value(0));

    String attemptId = learningQuestionAttemptRepository.findAll().get(0).getId();

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts/{attemptId}/disputes", material.getId(), attemptId)
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "questionIndex": 0,
            "selectedOptionIndex": 1,
            "reason": "A válaszom helyes volt, de a rendszer rosszul értékelt."
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("OPEN"))
      .andExpect(jsonPath("$.studentSubject").value("student-1"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-disputes", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].status").value("OPEN"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.progressEntries[0].status").value("SUBMITTED"));

    String disputeId = learningQuestionDisputeRepository.findAll().get(0).getId();

    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-disputes/{disputeId}/review", material.getId(), disputeId)
        .with(jwt().jwt(token -> token.subject("teacher-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "status": "ACCEPTED",
            "reviewReason": "Elfogadva, a válasz helyes volt.",
            "overrideScore": 2
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ACCEPTED"))
      .andExpect(jsonPath("$.overrideScore").value(2));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-attempts", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].score").value(2));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.progressEntries[0].status").value("COMPLETED"))
      .andExpect(jsonPath("$.progressEntries[0].score").value(2));
  }

  @Test
  void studentSeesOnlyOwnDisputesAndCannotReview() throws Exception {
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
          {"generationId": "%s", "answers": [{"questionIndex": 0, "selectedOptionIndexes": [1]}]}
          """.formatted(questionSet.getId())))
      .andExpect(status().isCreated());

    String attemptId = learningQuestionAttemptRepository.findAll().get(0).getId();
    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-attempts/{attemptId}/disputes", material.getId(), attemptId)
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "questionIndex": 0,
            "selectedOptionIndex": 1,
            "reason": "Saját hibabejelentés."
          }
          """))
      .andExpect(status().isCreated());

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/question-disputes", material.getId())
        .with(jwt().jwt(token -> token.subject("student-2"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0]").doesNotExist());

    String disputeId = learningQuestionDisputeRepository.findAll().get(0).getId();
    mockMvc.perform(post("/api/v1/learning/materials/{materialId}/question-disputes/{disputeId}/review", material.getId(), disputeId)
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"status": "ACCEPTED", "reviewReason": "nem szabadna"}
          """))
      .andExpect(status().isForbidden());
  }
}
