package org.autoforge.backend.repository;

import java.util.List;
import java.util.Optional;
import org.autoforge.backend.domain.LearningMaterialSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningMaterialSourceRepository extends JpaRepository<LearningMaterialSource, String> {

  List<LearningMaterialSource> findByMaterialIdAndDeletedAtIsNullOrderByCreatedAtAsc(String materialId);

  Optional<LearningMaterialSource> findByIdAndDeletedAtIsNull(String id);
}
