package org.autoforge.backend.repository;

import java.util.Optional;
import org.autoforge.backend.domain.LearningLearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningLearnerProfileRepository extends JpaRepository<LearningLearnerProfile, String> {

  Optional<LearningLearnerProfile> findByOwnerSubject(String ownerSubject);
}
