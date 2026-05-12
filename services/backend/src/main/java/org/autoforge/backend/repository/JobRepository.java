package org.autoforge.backend.repository;

import java.util.List;
import java.util.Optional;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, String> {

  List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status);

  Optional<Job> findByJiraIssueKey(String jiraIssueKey);
}
