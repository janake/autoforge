package org.autoforge.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.domain.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
  "spring.datasource.url=jdbc:h2:mem:jobdb;MODE=Oracle;DB_CLOSE_DELAY=-1",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.datasource.username=sa",
  "spring.datasource.password=",
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class JobRepositoryTest {

  @Autowired
  private JobRepository jobRepository;

  @Test
  void savesAndLoadsJobWithGeneratedIdAndTimestamps() {
    Job job = new Job("AUTO-179", "Build job persistence", "autoforge/backend", "main", JobStatus.QUEUED);

    Job saved = jobRepository.saveAndFlush(job);

    assertThat(saved.getId()).isNotBlank();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(JobStatus.QUEUED);
    assertThat(jobRepository.findByJiraIssueKey("AUTO-179")).hasValueSatisfying(found -> {
      assertThat(found.getPrompt()).isEqualTo("Build job persistence");
      assertThat(found.getTargetRepository()).isEqualTo("autoforge/backend");
    });
  }

  @Test
  void findsQueuedJobsInCreationOrder() {
    jobRepository.saveAndFlush(new Job("AUTO-180", "First", "repo", "main", JobStatus.QUEUED));
    jobRepository.saveAndFlush(new Job("AUTO-181", "Second", "repo", "main", JobStatus.RUNNING));
    jobRepository.saveAndFlush(new Job("AUTO-182", "Third", "repo", "main", JobStatus.QUEUED));

    List<Job> queuedJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);

    assertThat(queuedJobs).extracting(Job::getJiraIssueKey).containsExactly("AUTO-180", "AUTO-182");
  }
}
