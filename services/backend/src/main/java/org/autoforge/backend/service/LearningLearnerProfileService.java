package org.autoforge.backend.service;

import java.util.Objects;
import org.autoforge.backend.domain.LearningLearnerProfile;
import org.autoforge.backend.dto.LearningLearnerProfileRequest;
import org.autoforge.backend.dto.LearningLearnerProfileResponse;
import org.autoforge.backend.repository.LearningLearnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningLearnerProfileService {

  private final LearningLearnerProfileRepository learningLearnerProfileRepository;

  public LearningLearnerProfileResponse getProfile(String subject) {
    return toResponse(loadOrCreate(subject));
  }

  @Transactional
  public LearningLearnerProfileResponse updateProfile(String subject, LearningLearnerProfileRequest request) {
    LearningLearnerProfile profile = loadOrCreate(subject);
    profile.update(
      normalize(request.knowledgeLevel()),
      normalize(request.preferredQuestionStyle()),
      normalize(request.preferredExplanationStyle()),
      normalize(request.studyGoal()),
      normalize(request.promptNotes())
    );
    return toResponse(profile);
  }

  public String buildRetrievalContext(String subject) {
    return toResponse(loadOrCreate(subject)).retrievalContext();
  }

  private LearningLearnerProfile loadOrCreate(String subject) {
    return learningLearnerProfileRepository.findByOwnerSubject(subject)
      .orElseGet(() -> learningLearnerProfileRepository.save(LearningLearnerProfile.create(subject)));
  }

  private LearningLearnerProfileResponse toResponse(LearningLearnerProfile profile) {
    return new LearningLearnerProfileResponse(
      profile.getOwnerSubject(),
      profile.getKnowledgeLevel(),
      profile.getPreferredQuestionStyle(),
      profile.getPreferredExplanationStyle(),
      profile.getStudyGoal(),
      profile.getPromptNotes(),
      buildContext(profile),
      profile.getCreatedAt(),
      profile.getUpdatedAt()
    );
  }

  private String buildContext(LearningLearnerProfile profile) {
    StringBuilder builder = new StringBuilder();
    builder.append("Use only learning materials owned by or assigned to the current subject.");
    builder.append(" Profile: level=").append(profile.getKnowledgeLevel());
    builder.append(", questionStyle=").append(profile.getPreferredQuestionStyle());
    builder.append(", explanationStyle=").append(profile.getPreferredExplanationStyle());
    if (profile.getStudyGoal() != null && !profile.getStudyGoal().isBlank()) {
      builder.append(", studyGoal=").append(profile.getStudyGoal());
    }
    if (profile.getPromptNotes() != null && !profile.getPromptNotes().isBlank()) {
      builder.append(", notes=").append(profile.getPromptNotes());
    }
    return builder.toString();
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isBlank() ? null : normalized;
  }
}
