package org.autoforge.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void createsQueuedJobFromValidJiraKey() throws Exception {
    mockMvc.perform(post("/api/v1/jobs")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "jiraIssueKey": "AUTO-242",
            "prompt": "Implement job intake",
            "targetRepository": "janake/autoforge",
            "baseBranch": "main"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.jiraIssueKey").value("AUTO-242"))
      .andExpect(jsonPath("$.status").value("QUEUED"));
  }

  @Test
  void rejectsInvalidJiraKey() throws Exception {
    mockMvc.perform(post("/api/v1/jobs")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "jiraIssueKey": "bad-key",
            "prompt": "Implement job intake",
            "targetRepository": "janake/autoforge",
            "baseBranch": "main"
          }
          """))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(400));
  }
}
