package org.autoforge.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningQuestionAttempt;
import org.autoforge.backend.dto.LearningContentGenerationResponse;
import org.autoforge.backend.dto.LearningQuestionAnswerRequest;
import org.autoforge.backend.dto.LearningQuestionAttemptRequest;
import org.autoforge.backend.dto.LearningQuestionAttemptResponse;
import org.autoforge.backend.dto.LearningQuestionSetPayload;
import org.autoforge.backend.repository.LearningGeneratedContentRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningQuestionAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningQuestionAttemptService {

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;
  private final LearningGeneratedContentRepository learningGeneratedContentRepository;
  private final LearningQuestionAttemptRepository learningQuestionAttemptRepository;
  private final LearningContentGenerationService learningContentGenerationService;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public List<LearningContentGenerationResponse> listQuestionSets(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    return learningGeneratedContentRepository
      .findByMaterialIdAndOwnerSubjectAndGenerationTypeOrderByCreatedAtDesc(materialId, material.getOwnerSubject(), LearningContentGenerationType.QUESTION_SET)
      .stream()
      .map(content -> learningContentGenerationService.toResponse(content))
      .toList();
  }

  @Transactional
  public LearningQuestionAttemptResponse submitAttempt(String materialId, String subject, Collection<String> groups, LearningQuestionAttemptRequest request) {
    loadAccessibleMaterial(materialId, subject, groups);
    LearningGeneratedContent generation = learningGeneratedContentRepository.findById(request.generationId())
      .filter(content -> Objects.equals(content.getMaterialId(), materialId))
      .filter(content -> content.getGenerationType() == LearningContentGenerationType.QUESTION_SET)
      .orElseThrow(() -> new LearningMaterialNotFoundException(request.generationId()));

    LearningQuestionSetPayload questionSet = readQuestionSet(generation.getStructuredContent());
    List<LearningQuestionAnswerRequest> answers = request.answers() == null ? List.of() : request.answers();
    int score = 0;
    for (LearningQuestionAnswerRequest answer : answers) {
      if (answer.questionIndex() >= 0
        && answer.questionIndex() < questionSet.questions().size()
        && questionSet.questions().get(answer.questionIndex()).correctOptionIndex() == answer.selectedOptionIndex()) {
        score++;
      }
    }

    LearningQuestionAttempt saved = learningQuestionAttemptRepository.save(LearningQuestionAttempt.create(
      materialId,
      generation.getId(),
      subject,
      score,
      questionSet.questions().size(),
      writeAnswers(answers)
    ));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<LearningQuestionAttemptResponse> listAttempts(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    List<LearningQuestionAttempt> attempts = Objects.equals(material.getOwnerSubject(), subject)
      ? learningQuestionAttemptRepository.findByMaterialIdOrderBySubmittedAtDesc(materialId)
      : learningQuestionAttemptRepository.findByMaterialIdAndStudentSubjectOrderBySubmittedAtDesc(materialId, subject);
    return attempts.stream().map(this::toResponse).toList();
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

  private LearningQuestionSetPayload readQuestionSet(String structuredContent) {
    try {
      return objectMapper.readValue(structuredContent, LearningQuestionSetPayload.class);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read learning question set", exception);
    }
  }

  private String writeAnswers(List<LearningQuestionAnswerRequest> answers) {
    try {
      return objectMapper.writeValueAsString(answers);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize learning question answers", exception);
    }
  }

  private LearningQuestionAttemptResponse toResponse(LearningQuestionAttempt attempt) {
    return new LearningQuestionAttemptResponse(
      attempt.getId(),
      attempt.getMaterialId(),
      attempt.getGenerationId(),
      attempt.getStudentSubject(),
      attempt.getScore(),
      attempt.getTotalQuestions(),
      attempt.getAnswers(),
      attempt.getSubmittedAt()
    );
  }
}
