package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.autoforge.backend.repository.JobRepository;
import org.autoforge.backend.repository.PromptDraftMessageRepository;
import org.autoforge.backend.repository.PromptDraftRepository;
import org.autoforge.backend.jira.CreateJiraIssueResponse;
import org.autoforge.backend.jira.JiraIssueClient;
import org.autoforge.backend.domain.JobStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
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

  @org.springframework.boot.test.mock.mockito.MockBean
  private JiraIssueClient jiraIssueClient;

  @BeforeEach
  void cleanState() {
    promptDraftMessageRepository.deleteAll();
    promptDraftRepository.deleteAll();
    jobRepository.deleteAll();
  }

  private static JwtRequestPostProcessor developerJwt() {
    return jwt()
      .jwt(token -> token.claim("groups", List.of("developer")))
      .authorities(new SimpleGrantedAuthority("ROLE_developer"));
  }

  private static JwtRequestPostProcessor teacherJwt() {
    return jwt()
      .jwt(token -> token.claim("groups", List.of("teacher")))
      .authorities(new SimpleGrantedAuthority("ROLE_teacher"));
  }

  @Test
  void createsClarifyingDraftWithoutJobs() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CLARIFYING"))
      .andExpect(jsonPath("$.intent").value("TASK"))
      .andExpect(jsonPath("$.readyForApproval").value(false))
      .andExpect(jsonPath("$.pendingQuestions").isArray())
      .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Which repository should this apply to?")))
      .andExpect(jsonPath("$.messages[0].role").value("USER"))
      .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"));

    assertThat(jobRepository.count()).isZero();
    assertThat(promptDraftRepository.count()).isEqualTo(1);
    assertThat(promptDraftMessageRepository.count()).isEqualTo(2);
  }

  @Test
  void rejectsPromptDraftAccessWithoutDeveloperGroup() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isForbidden());
  }

  @Test
  void allowsPromptDraftAccessForTeacherGroupMembers() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(teacherJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CLARIFYING"));
  }

  @Test
  void becomesReadyForApprovalAfterSufficientClarification() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
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
        .with(developerJwt())
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
        .with(developerJwt()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("READY_FOR_APPROVAL"))
      .andExpect(jsonPath("$.readyForApproval").value(true));
  }

  @Test
  void approvesReadyDraftAndRecordsApprover() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Implement prompt draft approval flow for janake/autoforge with explicit acceptance criteria and no automatic implementation before approval"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("READY_FOR_APPROVAL"))
      .andExpect(jsonPath("$.intent").value("FEATURE"))
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/approve", draftId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "selectedIntent": "FEATURE"
          }
          """)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("APPROVED"))
      .andExpect(jsonPath("$.intent").value("FEATURE"))
      .andExpect(jsonPath("$.selectedIntent").value("FEATURE"))
      .andExpect(jsonPath("$.approvedBy").value("janake"))
      .andExpect(jsonPath("$.approvedAt").isNotEmpty())
      .andExpect(jsonPath("$.readyForApproval").value(false));

    assertThat(jobRepository.count()).isZero();
  }

  @Test
  void rejectsApprovalWhenDraftIsNotReady() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Add something useful"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/approve", draftId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "selectedIntent": "FEATURE"
          }
          """)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.status").value(409))
      .andExpect(jsonPath("$.message").value("Prompt draft is not ready for approval: " + draftId));
  }

  @Test
  void rejectsEditingApprovedDraft() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Implement prompt draft approval flow for janake/autoforge with explicit acceptance criteria and no automatic implementation before approval"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/approve", draftId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "selectedIntent": "FEATURE"
          }
          """)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/messages", draftId)
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "content": "Can we change the scope?"
          }
          """))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message").value("Approved prompt draft cannot be edited: " + draftId));
  }

  @Test
  void createsJiraTicketOnlyFromApprovedDraftAndIsIdempotent() throws Exception {
    when(jiraIssueClient.createIssue(any())).thenReturn(new CreateJiraIssueResponse("AUTO-999", "https://autoforge.atlassian.net/browse/AUTO-999"));

    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Implement prompt draft approval flow for janake/autoforge with explicit acceptance criteria and no automatic implementation before approval"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/approve", draftId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "selectedIntent": "FEATURE"
          }
          """)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/jira-ticket", draftId)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("TICKET_CREATED"))
      .andExpect(jsonPath("$.jiraIssueKey").value("AUTO-999"))
      .andExpect(jsonPath("$.jiraIssueUrl").value("https://autoforge.atlassian.net/browse/AUTO-999"));

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/jira-ticket", draftId)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.jiraIssueKey").value("AUTO-999"));

    verify(jiraIssueClient, times(1)).createIssue(any());
    verifyNoMoreInteractions(jiraIssueClient);
  }

  @Test
  void createsImplementationJobOnlyAfterTicketCreation() throws Exception {
    when(jiraIssueClient.createIssue(any())).thenReturn(new CreateJiraIssueResponse("AUTO-999", "https://autoforge.atlassian.net/browse/AUTO-999"));

    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Implement prompt draft approval flow for janake/autoforge with explicit acceptance criteria and no automatic implementation before approval"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/approve", draftId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "selectedIntent": "FEATURE"
          }
          """)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/jira-ticket", draftId)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/job", draftId)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("QUEUED"))
      .andExpect(jsonPath("$.prUrl").value(""));

    assertThat(jobRepository.count()).isEqualTo(1);
    var job = jobRepository.findAll().getFirst();
    assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
    assertThat(job.getJiraIssueKey()).isEqualTo("AUTO-999");
  }

  @Test
  void rejectsJiraTicketCreationWhenDraftIsNotApproved() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/jira-ticket", draftId)
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message").value("Prompt draft is not approved: " + draftId));
  }

  @Test
  void rejectsImplementationJobCreationBeforeTicketCreation() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/job", draftId)
        .with(developerJwt()))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message").value("Prompt draft does not have a Jira ticket yet: " + draftId));
  }

  @Test
  void rejectsApprovalWithoutSelectedIntent() throws Exception {
    var created = mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(developerJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Implement prompt draft approval flow for janake/autoforge with explicit acceptance criteria and no automatic implementation before approval"
          }
          """))
      .andExpect(status().isCreated())
      .andReturn();

    String draftId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.draftId");

    mockMvc.perform(post("/api/v1/prompt-drafts/{draftId}/approve", draftId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}")
        .with(developerJwt().jwt(jwt -> jwt.claim("preferred_username", "janake"))))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message").value("Prompt draft approval requires a selected intent: " + draftId));
  }
}
