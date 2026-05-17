package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, String> {

  List<LearningMaterial> findByOwnerSubject(String ownerSubject);
}
