package org.autoforge.backend.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.autoforge.backend.repository.LearningLearnerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningLearnerProfileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningLearnerProfileRepository learningLearnerProfileRepository;

  @BeforeEach
  void cleanState() {
    learningLearnerProfileRepository.deleteAll();
  }

  @Test
  void returnsDefaultProfileAndSupportsPerUserUpdates() throws Exception {
    mockMvc.perform(get("/api/v1/learning/learner-profile")
        .with(jwt().jwt(token -> token.subject("student-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ownerSubject").value("student-1"))
      .andExpect(jsonPath("$.knowledgeLevel").value("beginner"))
      .andExpect(jsonPath("$.retrievalContext").value(containsString("level=beginner")));

    mockMvc.perform(put("/api/v1/learning/learner-profile")
        .with(jwt().jwt(token -> token.subject("student-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "knowledgeLevel": "intermediate",
            "preferredQuestionStyle": "scenario",
            "preferredExplanationStyle": "concise",
            "studyGoal": "Exam prep",
            "promptNotes": "Prefer short examples"
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.knowledgeLevel").value("intermediate"))
      .andExpect(jsonPath("$.preferredQuestionStyle").value("scenario"))
      .andExpect(jsonPath("$.retrievalContext").value(containsString("studyGoal=Exam prep")))
      .andExpect(jsonPath("$.retrievalContext").value(containsString("notes=Prefer short examples")));

    mockMvc.perform(get("/api/v1/learning/learner-profile")
        .with(jwt().jwt(token -> token.subject("student-2"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ownerSubject").value("student-2"))
      .andExpect(jsonPath("$.knowledgeLevel").value("beginner"));
  }
}
