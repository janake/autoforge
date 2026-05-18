package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningQuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningQuestionAttemptRepository extends JpaRepository<LearningQuestionAttempt, String> {

  List<LearningQuestionAttempt> findByMaterialIdOrderBySubmittedAtDesc(String materialId);

  List<LearningQuestionAttempt> findByMaterialIdAndStudentSubjectOrderBySubmittedAtDesc(String materialId, String studentSubject);
}
