package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningChunkRepository extends JpaRepository<LearningChunk, String> {

  List<LearningChunk> findByJobIdOrderByChunkIndexAsc(String jobId);

  void deleteByJobId(String jobId);

  long countByJobId(String jobId);
}
