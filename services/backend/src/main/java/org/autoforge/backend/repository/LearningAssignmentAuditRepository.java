package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningAssignmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningAssignmentAuditRepository extends JpaRepository<LearningAssignmentAudit, String> {

  List<LearningAssignmentAudit> findByMaterialIdOrderByCreatedAtDesc(String materialId);
}
