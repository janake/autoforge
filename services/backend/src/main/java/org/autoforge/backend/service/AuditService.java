package org.autoforge.backend.service;

import java.util.HashMap;
import java.util.Map;
import org.autoforge.backend.domain.AuditEventType;
import org.autoforge.backend.domain.AuditLog;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public AuditLog logEvent(String jobId, String userSubject, AuditEventType eventType, Object details) {
    String detailsJson = serializeDetails(jobId, details);
    return auditLogRepository.save(AuditLog.create(jobId, userSubject, eventType, detailsJson));
  }

  @Transactional
  public AuditLog logJobCreated(Job job, String userSubject, String prompt, String targetRepository, String baseBranch) {
    Map<String, String> details = new HashMap<>();
    details.put("prompt", prompt);
    details.put("targetRepository", targetRepository);
    details.put("baseBranch", baseBranch);
    details.put("status", job.getStatus().name());
    if (job.getJiraIssueKey() != null && !job.getJiraIssueKey().isBlank()) {
      details.put("jiraIssueKey", job.getJiraIssueKey());
    }

    return logEvent(job.getId(), userSubject, AuditEventType.JOB_CREATED, details);
  }

  @Transactional
  public AuditLog logJobStarted(Job job, String userSubject) {
    return logEvent(job.getId(), userSubject, AuditEventType.JOB_STARTED, Map.of(
      "status", job.getStatus().name()
    ));
  }

  @Transactional
  public AuditLog logPatchGenerated(Job job, String userSubject, String patchSummary) {
    return logEvent(job.getId(), userSubject, AuditEventType.PATCH_GENERATED, Map.of(
      "patchSummary", patchSummary
    ));
  }

  @Transactional
  public AuditLog logPrOpened(Job job, String userSubject, String prUrl) {
    return logEvent(job.getId(), userSubject, AuditEventType.PR_OPENED, Map.of(
      "prUrl", prUrl
    ));
  }

  @Transactional
  public AuditLog logJobFailed(Job job, String userSubject, String reason) {
    return logEvent(job.getId(), userSubject, AuditEventType.JOB_FAILED, Map.of(
      "reason", reason
    ));
  }

  private String serializeDetails(String jobId, Object details) {
    try {
      return objectMapper.writeValueAsString(details == null ? Map.of() : details);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialize audit details for job {}", jobId, ex);
      return "{}";
    }
  }
}
