package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.autoforge.backend.repository.JobRepository;
import org.autoforge.backend.repository.PromptDraftMessageRepository;
import org.autoforge.backend.repository.PromptDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PromptDraftControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JobRepository jobRepository;

  @Autowired
  private PromptDraftRepository promptDraftRepository;

  @Autowired
  private PromptDraftMessageRepository promptDraftMessageRepository;

  @BeforeEach
  void cleanState() {
    promptDraftMessageRepository.deleteAll();
    promptDraftRepository.deleteAll();
    jobRepository.deleteAll();
  }

  @Test
  void createsClarifyingDraftWithoutJobs() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Add something useful"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CLARIFYING"))
      .andExpect(jsonPath("$.readyForApproval").value(false))
      .andExpect(jsonPath("$.pendingQuestions").isArray())
      .andExpect(jsonPath("$.messages[0].role").value("USER"))
      .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"));

    assertThat(jobRepository.count()).isZero();
    assertThat(promptDraftRepository.count()).isEqualTo(1);
    assertThat(promptDraftMessageRepository.count()).isEqualTo(2);
  }

  @Test
  void becomesReadyForApprovalAfterSufficientClarification() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Add something useful"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/messages", draftId)
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "content": "Repo: janake/autoforge. Acceptance: capture clarifying questions until the work is ready."
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("READY_FOR_APPROVAL"))
      .andExpect(jsonPath("$.readyForApproval").value(true))
      .andExpect(jsonPath("$.pendingQuestions").isEmpty())
      .andExpect(jsonPath("$.messages[2].role").value("USER"))
      .andExpect(jsonPath("$.messages[3].role").value("ASSISTANT"));

    mockMvc.perform(get("/api/v1/prompt-drafts/{draftId}", draftId)
        .with(jwt()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("READY_FOR_APPROVAL"))
      .andExpect(jsonPath("$.readyForApproval").value(true));
  }
}
