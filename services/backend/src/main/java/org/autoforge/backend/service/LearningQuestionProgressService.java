package org.autoforge.backend.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningQuestionDisputeStatus;
import org.autoforge.backend.domain.LearningQuestionProgress;
import org.autoforge.backend.domain.LearningQuestionProgressStatus;
import org.autoforge.backend.dto.LearningQuestionProgressResponse;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningQuestionProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningQuestionProgressService {

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;
  private final LearningQuestionProgressRepository learningQuestionProgressRepository;

  @Transactional(readOnly = true)
  public List<LearningQuestionProgressResponse> listProgress(String materialId, String subject, Collection<String> groups, boolean canSeeAll) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    List<LearningQuestionProgress> progress = canSeeAll || Objects.equals(material.getOwnerSubject(), subject)
      ? learningQuestionProgressRepository.findByMaterialIdOrderByUpdatedAtDesc(materialId)
      : learningQuestionProgressRepository.findByMaterialIdAndStudentSubjectOrderByUpdatedAtDesc(materialId, subject);
    return progress.stream().map(this::toResponse).toList();
  }

  @Transactional
  public void markStarted(String materialId, String generationId, String subject, Collection<String> groups) {
    loadAccessibleMaterial(materialId, subject, groups);
    LearningQuestionProgress progress = loadOrCreate(materialId, generationId, subject);
    progress.setStudentGroups(normalizeGroups(groups));
    progress.start();
    learningQuestionProgressRepository.save(progress);
  }

  @Transactional
  public void markSubmitted(String materialId, String generationId, String subject, Collection<String> groups, String attemptId, int score, int totalQuestions) {
    LearningQuestionProgress progress = loadOrCreate(materialId, generationId, subject);
    progress.setStudentGroups(normalizeGroups(groups));
    progress.submit(attemptId, score, totalQuestions);
    learningQuestionProgressRepository.save(progress);
  }

  @Transactional
  public void markReviewed(String materialId, String generationId, String subject, LearningQuestionDisputeStatus reviewStatus, Integer overrideScore, Integer score, Integer totalQuestions) {
    LearningQuestionProgress progress = loadOrCreate(materialId, generationId, subject);
    progress.review(reviewStatus == LearningQuestionDisputeStatus.ACCEPTED, overrideScore, score, totalQuestions);
    learningQuestionProgressRepository.save(progress);
  }

  private LearningQuestionProgress loadOrCreate(String materialId, String generationId, String subject) {
    return learningQuestionProgressRepository.findByMaterialIdAndGenerationIdAndStudentSubject(materialId, generationId, subject)
      .orElseGet(() -> LearningQuestionProgress.create(materialId, generationId, subject));
  }

  private List<String> parseGroups(String groups) {
    if (groups == null || groups.isBlank()) {
      return List.of();
    }

    return List.of(groups.split(","));
  }

  private LearningMaterial loadAccessibleMaterial(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = learningMaterialRepository.findById(materialId).orElseThrow(() -> new LearningMaterialNotFoundException(materialId));
    if (Objects.equals(material.getOwnerSubject(), subject)) {
      return material;
    }
    Set<String> normalizedGroups = normalizeGroups(groups);
    for (LearningMaterialAssignment assignment : learningMaterialAssignmentRepository.findByMaterialId(materialId)) {
      if (assignment.getTargetType() == LearningAssignmentTargetType.STUDENT && Objects.equals(assignment.getTargetIdentifier(), subject)) {
        return material;
      }
      if (assignment.getTargetType() == LearningAssignmentTargetType.GROUP && normalizedGroups.contains(assignment.getTargetIdentifier())) {
        return material;
      }
    }
    throw new LearningMaterialAccessDeniedException(materialId);
  }

  private Set<String> normalizeGroups(Collection<String> groups) {
    return groups == null ? Set.of() : groups.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(value -> !value.isBlank())
      .map(value -> value.startsWith("/") ? value.substring(1) : value)
      .map(value -> value.contains("/") ? value.substring(value.lastIndexOf('/') + 1) : value)
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private LearningQuestionProgressResponse toResponse(LearningQuestionProgress progress) {
    return new LearningQuestionProgressResponse(
      progress.getId(),
      progress.getMaterialId(),
      progress.getGenerationId(),
      progress.getStudentSubject(),
      parseGroups(progress.getStudentGroups()),
      progress.getStatus(),
      progress.getAttemptId(),
      progress.getAttemptCount(),
      progress.getScore(),
      progress.getTotalQuestions(),
      progress.getCreatedAt(),
      progress.getUpdatedAt(),
      progress.getStartedAt(),
      progress.getSubmittedAt(),
      progress.getReviewedAt(),
      progress.getCompletedAt()
    );
  }
}
