package org.autoforge.backend.repository;

import java.util.Optional;
import org.autoforge.backend.domain.LearningIngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningIngestionJobRepository extends JpaRepository<LearningIngestionJob, String> {

  Optional<LearningIngestionJob> findByMaterialId(String materialId);
}
