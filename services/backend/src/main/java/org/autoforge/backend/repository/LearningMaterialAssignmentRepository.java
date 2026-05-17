package org.autoforge.backend.repository;

import java.util.Collection;
import java.util.List;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningMaterialAssignmentRepository extends JpaRepository<LearningMaterialAssignment, String> {

  List<LearningMaterialAssignment> findByMaterialId(String materialId);

  List<LearningMaterialAssignment> findByTargetTypeAndTargetIdentifierIn(LearningAssignmentTargetType targetType, Collection<String> targetIdentifiers);

  void deleteByMaterialId(String materialId);
}
