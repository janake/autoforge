package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.autoforge.backend.repository.AuditLogRepository;
import org.autoforge.backend.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
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

  @Autowired
  private JobRepository jobRepository;

  @Autowired
  private AuditLogRepository auditLogRepository;

  @BeforeEach
  void cleanJobs() {
    jobRepository.deleteAll();
    auditLogRepository.deleteAll();
  }

  @Test
  void createsQueuedJobFromValidRequestAndPersistsIt() throws Exception {
    mockMvc.perform(post("/api/v1/jobs")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Implement job creation API"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.jobId").isNotEmpty())
      .andExpect(jsonPath("$.status").value("QUEUED"));

    var job = jobRepository.findAll().getFirst();
    assertThat(job.getJiraIssueKey()).isNull();
    assertThat(job.getPrompt()).isEqualTo("Implement job creation API");
    assertThat(job.getTargetRepository()).isEqualTo("prodet/autoforge");
    assertThat(job.getBaseBranch()).isEqualTo("main");
    assertThat(job.getStatus().name()).isEqualTo("QUEUED");

    var auditLogs = auditLogRepository.findByJobIdOrderByTimestampAsc(job.getId());
    assertThat(auditLogs).hasSize(1);
    assertThat(auditLogs.get(0).getEventType().name()).isEqualTo("JOB_CREATED");
  }

  @Test
  void ignoresClientSuppliedJiraKeyAndRepositoryFields() throws Exception {
    mockMvc.perform(post("/api/v1/jobs")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "jiraIssueKey": "bad-key",
            "prompt": "Implement job intake",
            "targetRepository": "attacker/repo",
            "baseBranch": "feature/evil"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("QUEUED"));

    var job = jobRepository.findAll().getFirst();
    assertThat(job.getJiraIssueKey()).isNull();
    assertThat(job.getTargetRepository()).isEqualTo("prodet/autoforge");
    assertThat(job.getBaseBranch()).isEqualTo("main");
  }

  @Test
  void rejectsBlankPrompt() throws Exception {
    mockMvc.perform(post("/api/v1/jobs")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": ""
          }
          """))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void returnsJobById() throws Exception {
    var saved = jobRepository.saveAndFlush(
      org.autoforge.backend.domain.Job.createQueued("AUTO-187", "Check job status", "janake/autoforge", "main")
    );

    mockMvc.perform(get("/api/v1/jobs/{jobId}", saved.getId())
        .with(jwt()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.jobId").value(saved.getId()))
      .andExpect(jsonPath("$.prompt").value("Check job status"))
      .andExpect(jsonPath("$.status").value("QUEUED"))
      .andExpect(jsonPath("$.targetRepository").value("janake/autoforge"))
      .andExpect(jsonPath("$.baseBranch").value("main"));
  }

  @Test
  void returnsNotFoundForMissingJob() throws Exception {
    mockMvc.perform(get("/api/v1/jobs/{jobId}", "missing-job-id")
        .with(jwt()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.status").value(404));
  }
}
