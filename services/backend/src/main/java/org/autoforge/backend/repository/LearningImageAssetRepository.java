package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningImageAssetRepository extends JpaRepository<LearningImageAsset, String> {

  List<LearningImageAsset> findByMaterialIdOrderByCreatedAtAsc(String materialId);
}
