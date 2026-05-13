package org.autoforge.backend.service;

import java.util.List;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.domain.JobStatus;
import org.autoforge.backend.dto.GeneratedPatchResponse;
import org.autoforge.backend.git.CreatePullRequestRequest;
import org.autoforge.backend.git.CreatePullRequestResponse;
import org.autoforge.backend.git.GitPatchApplicationService;
import org.autoforge.backend.git.GitPublishService;
import org.autoforge.backend.git.GitRepositoryPreparationService;
import org.autoforge.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "autoforge.mvp.job-processor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JobProcessorService {

  private static final String SYSTEM_USER = "job-processor";

  private final JobRepository jobRepository;
  private final JobService jobService;
  private final AIPatchGenerator aiPatchGenerator;
  private final GitRepositoryPreparationService gitRepositoryPreparationService;
  private final GitPatchApplicationService gitPatchApplicationService;
  private final GitPublishService gitPublishService;

  @Scheduled(fixedDelayString = "${autoforge.mvp.job-processor.poll-interval-ms:5000}")
  public void pollQueuedJobs() {
    List<Job> queuedJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);

    for (Job job : queuedJobs) {
      processJob(job);
    }
  }

  void processJob(Job job) {
    Job currentJob = jobService.markRunning(job, SYSTEM_USER);

    try {
      GeneratedPatchResponse generatedPatch = aiPatchGenerator.generatePatch(currentJob);
      currentJob = jobService.markPatchGenerated(currentJob, SYSTEM_USER, generatedPatch.summary());

      CreatePullRequestRequest request = createPullRequestRequest(currentJob, generatedPatch);
      try (GitRepositoryPreparationService.GitRepositoryWorkspace workspace = gitRepositoryPreparationService.prepareRepository(request)) {
        GitPatchApplicationService.GitCommitResult commitResult = gitPatchApplicationService.applyPatchAndCommit(workspace, request);
        CreatePullRequestResponse pullRequestResponse = gitPublishService.pushBranchAndOpenPr(workspace, request, commitResult);
        jobService.markPrOpened(currentJob, SYSTEM_USER, pullRequestResponse.prUrl());
      }
    } catch (Exception exception) {
      jobService.markFailed(currentJob, SYSTEM_USER, failureReason(exception));
    }
  }

  private CreatePullRequestRequest createPullRequestRequest(Job job, GeneratedPatchResponse generatedPatch) {
    String reference = jobReference(job);
    String branchName = "autoforge/%s-job".formatted(reference);

    return new CreatePullRequestRequest(
      toRepositoryUrl(job.getTargetRepository()),
      job.getBaseBranch(),
      branchName,
      "%s Apply generated patch".formatted(reference),
      "%s: %s".formatted(reference, generatedPatch.summary()),
      job.getPrompt(),
      generatedPatch.patch()
    );
  }

  private String jobReference(Job job) {
    if (job.getJiraIssueKey() != null && !job.getJiraIssueKey().isBlank()) {
      return job.getJiraIssueKey();
    }

    return "JOB-%s".formatted(job.getId().substring(0, 8));
  }

  private String toRepositoryUrl(String targetRepository) {
    if (
      targetRepository.startsWith("http://")
        || targetRepository.startsWith("https://")
        || targetRepository.startsWith("git@")
        || targetRepository.startsWith("file:")
        || targetRepository.contains("://")
    ) {
      return targetRepository;
    }

    return "https://github.com/%s.git".formatted(targetRepository);
  }

  private String failureReason(Exception exception) {
    String message = exception.getMessage();
    return (message == null || message.isBlank()) ? exception.getClass().getSimpleName() : message;
  }
}
