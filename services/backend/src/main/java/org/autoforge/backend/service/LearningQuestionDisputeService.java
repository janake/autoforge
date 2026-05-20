package org.autoforge.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningQuestionAttempt;
import org.autoforge.backend.domain.LearningQuestionDispute;
import org.autoforge.backend.domain.LearningQuestionDisputeStatus;
import org.autoforge.backend.dto.LearningQuestionDisputeRequest;
import org.autoforge.backend.dto.LearningQuestionDisputeReviewRequest;
import org.autoforge.backend.dto.LearningQuestionDisputeResponse;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningQuestionAttemptRepository;
import org.autoforge.backend.repository.LearningQuestionDisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.autoforge.backend.service.LearningMaterialAccessDeniedException;
import org.autoforge.backend.service.LearningMaterialNotFoundException;

@Service
@RequiredArgsConstructor
public class LearningQuestionDisputeService {

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;
  private final LearningQuestionAttemptRepository learningQuestionAttemptRepository;
  private final LearningQuestionDisputeRepository learningQuestionDisputeRepository;
  private final LearningQuestionProgressService learningQuestionProgressService;

  @Transactional(readOnly = true)
  public List<LearningQuestionDisputeResponse> listDisputes(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    List<LearningQuestionDispute> disputes = Objects.equals(material.getOwnerSubject(), subject)
      ? learningQuestionDisputeRepository.findByMaterialIdOrderByCreatedAtDesc(materialId)
      : learningQuestionDisputeRepository.findByMaterialIdAndStudentSubjectOrderByCreatedAtDesc(materialId, subject);
    return disputes.stream().map(this::toResponse).toList();
  }

  @Transactional
  public LearningQuestionDisputeResponse createDispute(String materialId, String subject, Collection<String> groups, String attemptId, LearningQuestionDisputeRequest request) {
    loadAccessibleMaterial(materialId, subject, groups);
    LearningQuestionAttempt attempt = loadAttemptForStudent(materialId, attemptId, subject);
    if (request.reason() == null || request.reason().isBlank()) {
      throw new IllegalArgumentException("Dispute reason is required");
    }

    LearningQuestionDispute dispute = learningQuestionDisputeRepository.save(LearningQuestionDispute.create(
      materialId,
      attempt.getId(),
      subject,
      request.questionIndex(),
      request.selectedOptionIndex(),
      request.reason().trim()
    ));
    return toResponse(dispute);
  }

  @Transactional
  public LearningQuestionDisputeResponse reviewDispute(String materialId, String subject, Collection<String> groups, String disputeId, LearningQuestionDisputeReviewRequest request) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    LearningQuestionDispute dispute = learningQuestionDisputeRepository.findById(disputeId)
      .filter(existing -> Objects.equals(existing.getMaterialId(), materialId))
      .orElseThrow(() -> new LearningQuestionDisputeNotFoundException(disputeId));

    if (request.status() == null || request.status() == LearningQuestionDisputeStatus.OPEN) {
      throw new IllegalArgumentException("Review status must be ACCEPTED or REJECTED");
    }

    dispute.review(subject, request.status(), request.reviewReason(), request.overrideScore());
    LearningQuestionAttempt attempt = learningQuestionAttemptRepository.findById(dispute.getAttemptId())
      .orElseThrow(() -> new LearningMaterialNotFoundException(dispute.getAttemptId()));
    if (request.status() == LearningQuestionDisputeStatus.ACCEPTED && request.overrideScore() != null) {
      attempt.setScore(request.overrideScore());
    }
    learningQuestionProgressService.markReviewed(
      materialId,
      attempt.getGenerationId(),
      attempt.getStudentSubject(),
      request.status(),
      request.overrideScore(),
      attempt.getScore(),
      attempt.getTotalQuestions()
    );
    return toResponse(dispute);
  }

  private LearningQuestionAttempt loadAttemptForStudent(String materialId, String attemptId, String subject) {
    LearningQuestionAttempt attempt = learningQuestionAttemptRepository.findById(attemptId)
      .orElseThrow(() -> new LearningMaterialNotFoundException(attemptId));
    if (!Objects.equals(attempt.getMaterialId(), materialId) || !Objects.equals(attempt.getStudentSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }
    return attempt;
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
      .collect(Collectors.toSet());
  }

  private LearningQuestionDisputeResponse toResponse(LearningQuestionDispute dispute) {
    return new LearningQuestionDisputeResponse(
      dispute.getId(),
      dispute.getMaterialId(),
      dispute.getAttemptId(),
      dispute.getStudentSubject(),
      dispute.getQuestionIndex(),
      dispute.getSelectedOptionIndex(),
      dispute.getReason(),
      dispute.getStatus(),
      dispute.getReviewerSubject(),
      dispute.getReviewReason(),
      dispute.getOverrideScore(),
      dispute.getCreatedAt(),
      dispute.getReviewedAt()
    );
  }
}
