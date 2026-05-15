package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.autoforge.backend.domain.Job;
import org.autoforge.backend.domain.JobStatus;
import org.junit.jupiter.api.Test;

class BranchNamingServiceTest {

  private final BranchNamingService branchNamingService = new BranchNamingService();

  @Test
  void usesFeaturePrefixForFeatureLikePromptsAndJiraKey() {
    Job job = new Job("AUTO-123", "Implement Jira ticket creation gate", "org/repo", "main", JobStatus.QUEUED);

    assertThat(branchNamingService.branchNameFor(job)).isEqualTo("feature/AUTO-123-implement-jira-ticket-creation-gate");
  }

  @Test
  void usesBugPrefixForBugLikePromptsAndTruncatesSlug() {
    Job job = new Job(null, "Fix a very long broken flow with many repeated repeated repeated repeated repeated repeated words", "org/repo", "main", JobStatus.QUEUED);

    assertThat(branchNamingService.branchNameFor(job))
      .startsWith("bug/JOB-")
      .matches("bug/JOB-[A-Za-z0-9]+-[a-z0-9-]{1,48}");
  }

  @Test
  void defaultsToTaskPrefixWhenNoStrongerIntentExists() {
    Job job = new Job(null, "Audit the workflow", "org/repo", "main", JobStatus.QUEUED);

    assertThat(branchNamingService.branchNameFor(job)).startsWith("task/JOB-");
  }
}
