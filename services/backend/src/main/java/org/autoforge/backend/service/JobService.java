package org.autoforge.backend.service;

import java.util.regex.Pattern;
import org.autoforge.backend.domain.Job;
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

  private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]+-[0-9]+$");

  private final JobRepository jobRepository;
  private final AuditService auditService;

  @Transactional
  public CreateJobResponse createJob(CreateJobRequest request) {
    if (!JIRA_KEY_PATTERN.matcher(request.jiraIssueKey()).matches()) {
      throw new IllegalArgumentException("Invalid Jira issue key");
    }

    Job saved = jobRepository.save(Job.createQueued(
      request.jiraIssueKey(),
      request.prompt(),
      request.targetRepository(),
      request.baseBranch()
    ));

    auditService.logJobCreated(saved, "system", request.prompt(), request.targetRepository(), request.baseBranch());

    return new CreateJobResponse(
      saved.getId(),
      saved.getJiraIssueKey(),
      saved.getStatus().name()
    );
  }

  @Transactional(readOnly = true)
  public JobResponse getJob(String jobId) {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));

    return new JobResponse(
      job.getId(),
      job.getJiraIssueKey(),
      job.getPrompt(),
      job.getTargetRepository(),
      job.getBaseBranch(),
      job.getStatus().name(),
      job.getPrUrl(),
      job.getErrorMessage(),
      job.getCreatedAt(),
      job.getUpdatedAt()
    );
  }
}
