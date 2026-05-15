package org.autoforge.backend.service;

import org.autoforge.backend.config.BackendMvpProperties;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.domain.JobStatus;
import org.autoforge.backend.domain.PromptDraft;
import org.autoforge.backend.dto.CreateJobRequest;
import org.autoforge.backend.dto.CreateJobResponse;
import org.autoforge.backend.dto.JobResponse;
import org.autoforge.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {

  private final JobRepository jobRepository;
  private final AuditService auditService;
  private final BackendMvpProperties properties;

  @Transactional
  public CreateJobResponse createJob(CreateJobRequest request) {
    Job saved = createQueuedJob(null, request.prompt());

    auditService.logJobCreated(saved, "system", request.prompt(), saved.getTargetRepository(), saved.getBaseBranch());

    return new CreateJobResponse(
      saved.getId(),
      saved.getStatus().name()
    );
  }

  @Transactional
  public JobResponse createJobFromPromptDraft(PromptDraft draft) {
    if (draft.getJiraIssueKey() == null || draft.getJiraIssueKey().isBlank()) {
      throw new IllegalStateException("Prompt draft does not have a Jira issue key: " + draft.getId());
    }

    Job saved = createQueuedJob(draft.getJiraIssueKey(), draft.getPrompt());
    auditService.logJobCreated(saved, "system", draft.getPrompt(), saved.getTargetRepository(), saved.getBaseBranch());

    return new JobResponse(
      saved.getId(),
      saved.getPrompt(),
      saved.getTargetRepository(),
      saved.getBaseBranch(),
      saved.getStatus().name(),
      saved.getPrUrl() == null ? "" : saved.getPrUrl(),
      saved.getErrorMessage(),
      saved.getCreatedAt(),
      saved.getUpdatedAt()
    );
  }

  @Transactional(readOnly = true)
  public JobResponse getJob(String jobId) {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));

    return new JobResponse(
      job.getId(),
      job.getPrompt(),
      job.getTargetRepository(),
      job.getBaseBranch(),
      job.getStatus().name(),
      job.getPrUrl() == null ? "" : job.getPrUrl(),
      job.getErrorMessage(),
      job.getCreatedAt(),
      job.getUpdatedAt()
    );
  }

  private String configuredTargetRepository() {
    BackendMvpProperties.Github github = properties.github();
    if (github == null || github.owner() == null || github.owner().isBlank()) {
      throw new IllegalStateException("Missing AUTOFORGE_GITHUB_OWNER configuration");
    }
    if (github.repo() == null || github.repo().isBlank()) {
      throw new IllegalStateException("Missing AUTOFORGE_GITHUB_REPO configuration");
    }

    return "%s/%s".formatted(github.owner(), github.repo());
  }

  private String configuredBaseBranch() {
    String baseBranch = properties.github() == null ? null : properties.github().baseBranch();
    return (baseBranch == null || baseBranch.isBlank()) ? "main" : baseBranch;
  }

  private Job createQueuedJob(String jiraIssueKey, String prompt) {
    return jobRepository.save(Job.createQueued(
      jiraIssueKey,
      prompt,
      configuredTargetRepository(),
      configuredBaseBranch()
    ));
  }

  @Transactional
  public Job markRunning(Job job, String userSubject) {
    job.setStatus(JobStatus.RUNNING);
    Job saved = jobRepository.save(job);
    auditService.logJobStarted(saved, userSubject);
    return saved;
  }

  @Transactional
  public Job markPatchGenerated(Job job, String userSubject, String patchSummary) {
    job.setStatus(JobStatus.PATCH_GENERATED);
    Job saved = jobRepository.save(job);
    auditService.logPatchGenerated(saved, userSubject, patchSummary);
    return saved;
  }

  @Transactional
  public Job markPrOpened(Job job, String userSubject, String prUrl) {
    job.setStatus(JobStatus.PR_OPENED);
    job.setPrUrl(prUrl);
    Job saved = jobRepository.save(job);
    auditService.logPrOpened(saved, userSubject, prUrl);
    return saved;
  }

  @Transactional
  public Job markFailed(Job job, String userSubject, String reason) {
    job.setStatus(JobStatus.FAILED);
    job.setErrorMessage(reason);
    Job saved = jobRepository.save(job);
    auditService.logJobFailed(saved, userSubject, reason);
    return saved;
  }
}
