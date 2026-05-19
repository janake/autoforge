package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningQuestionSetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningGeneratedContentRepository extends JpaRepository<LearningGeneratedContent, String> {

  List<LearningGeneratedContent> findByMaterialIdAndOwnerSubjectAndGenerationTypeOrderByCreatedAtDesc(String materialId, String ownerSubject, LearningContentGenerationType generationType);

  List<LearningGeneratedContent> findByMaterialIdAndOwnerSubjectOrderByCreatedAtDesc(String materialId, String ownerSubject);

  List<LearningGeneratedContent> findByMaterialIdAndOwnerSubjectAndGenerationTypeAndQuestionSetStatusOrderByCreatedAtDesc(String materialId, String ownerSubject, LearningContentGenerationType generationType, LearningQuestionSetStatus questionSetStatus);
}
