package org.autoforge.backend.repository;

import java.util.List;
import java.util.Optional;
import org.autoforge.backend.domain.LearningQuestionProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningQuestionProgressRepository extends JpaRepository<LearningQuestionProgress, String> {
  List<LearningQuestionProgress> findByMaterialIdOrderByUpdatedAtDesc(String materialId);

  List<LearningQuestionProgress> findByMaterialIdAndStudentSubjectOrderByUpdatedAtDesc(String materialId, String studentSubject);

  Optional<LearningQuestionProgress> findByMaterialIdAndGenerationIdAndStudentSubject(String materialId, String generationId, String studentSubject);
}
