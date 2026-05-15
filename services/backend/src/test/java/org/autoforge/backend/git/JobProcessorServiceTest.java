package org.autoforge.backend.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import org.mockito.ArgumentCaptor;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.domain.JobStatus;
import org.autoforge.backend.dto.GeneratedPatchResponse;
import org.autoforge.backend.service.AIPatchGenerator;
import org.autoforge.backend.service.JobProcessorService;
import org.autoforge.backend.repository.AuditLogRepository;
import org.autoforge.backend.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
  "autoforge.mvp.job-processor.enabled=true",
  "autoforge.mvp.job-processor.poll-interval-ms=99999999"
})
class JobProcessorServiceTest {

  @Autowired
  private JobProcessorService jobProcessorService;

  @Autowired
  private JobRepository jobRepository;

  @Autowired
  private AuditLogRepository auditLogRepository;

  @MockBean
  private AIPatchGenerator aiPatchGenerator;

  @MockBean
  private GitPublishService gitPublishService;

  @BeforeEach
  void cleanDatabase() {
    auditLogRepository.deleteAll();
    jobRepository.deleteAll();
  }

  @Test
  void processesQueuedJobIntoPrOpened() throws Exception {
    Path sourceRepository = Files.createTempDirectory("autoforge-job-processor-source-");
    try {
      GitProcessRunner.run(sourceRepository, "git", "init", "-b", "main");
      Files.writeString(sourceRepository.resolve("README.md"), "initial\n");
      GitProcessRunner.run(sourceRepository, "git", "add", "README.md");
      GitProcessRunner.run(
        sourceRepository,
        "git",
        "-c",
        "user.name=Autoforge Test",
        "-c",
        "user.email=test@example.com",
        "commit",
        "-m",
        "Initial commit"
      );

      Job job = jobRepository.saveAndFlush(Job.createQueued(
        "AUTO-281",
        "Process queued job",
        sourceRepository.toUri().toString(),
        "main"
      ));

      when(aiPatchGenerator.generatePatch(any())).thenReturn(new GeneratedPatchResponse(
        "diff --git a/README.md b/README.md\n--- a/README.md\n+++ b/README.md\n@@ -1 +1,2 @@\n initial\n+processed\n",
        "Add processed line",
        java.util.List.of("README.md")
      ));
      when(gitPublishService.pushBranchAndOpenPr(any(), any(), any())).thenAnswer(invocation -> {
        CreatePullRequestRequest request = invocation.getArgument(1);
        GitPatchApplicationService.GitCommitResult commitResult = invocation.getArgument(2);
        return new CreatePullRequestResponse("https://github.com/org/repo/pull/123", request.branchName(), commitResult.commitSha());
      });

      jobProcessorService.pollQueuedJobs();

      Job updated = jobRepository.findById(job.getId()).orElseThrow();
      assertThat(updated.getStatus()).isEqualTo(JobStatus.PR_OPENED);
      assertThat(updated.getPrUrl()).isEqualTo("https://github.com/org/repo/pull/123");
      ArgumentCaptor<CreatePullRequestRequest> requestCaptor = ArgumentCaptor.forClass(CreatePullRequestRequest.class);
      verify(gitPublishService).pushBranchAndOpenPr(any(), requestCaptor.capture(), any());
      assertThat(requestCaptor.getValue().branchName()).startsWith("task/AUTO-281-");
    } finally {
      GitPaths.deleteRecursively(sourceRepository);
    }
  }

  @Test
  void marksQueuedJobFailedWhenPatchGenerationFails() {
    Job job = jobRepository.saveAndFlush(Job.createQueued(
      "AUTO-211",
      "Process queued job",
      "janake/autoforge",
      "main"
    ));

    when(aiPatchGenerator.generatePatch(any())).thenThrow(new IllegalStateException("patch generator unavailable"));

    jobProcessorService.pollQueuedJobs();

    Job updated = jobRepository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(JobStatus.FAILED);
    assertThat(updated.getErrorMessage()).contains("patch generator unavailable");
  }

  @Test
  void ignoresQueuedJobsWithoutJiraIssueKey() {
    Job job = jobRepository.saveAndFlush(Job.createQueued(
      "Process queued job",
      "janake/autoforge",
      "main"
    ));

    jobProcessorService.pollQueuedJobs();

    Job updated = jobRepository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(JobStatus.QUEUED);
    assertThat(updated.getPrUrl()).isNull();
    assertThat(updated.getErrorMessage()).isNull();
  }
}
