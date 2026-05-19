package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningQuestionDispute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningQuestionDisputeRepository extends JpaRepository<LearningQuestionDispute, String> {

  List<LearningQuestionDispute> findByMaterialIdOrderByCreatedAtDesc(String materialId);

  List<LearningQuestionDispute> findByMaterialIdAndStudentSubjectOrderByCreatedAtDesc(String materialId, String studentSubject);
}
