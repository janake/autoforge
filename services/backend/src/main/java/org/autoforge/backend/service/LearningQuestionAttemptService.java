package org.autoforge.backend.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final LearningQuestionProgressService learningQuestionProgressService;
  private final ObjectMapper objectMapper;

  @Transactional
  public List<LearningContentGenerationResponse> listQuestionSets(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    List<LearningGeneratedContent> questionSets = learningGeneratedContentRepository
      .findByMaterialIdAndOwnerSubjectAndGenerationTypeOrderByCreatedAtDesc(materialId, material.getOwnerSubject(), LearningContentGenerationType.QUESTION_SET)
      .stream()
      .filter(content -> learningContentGenerationService.isQuestionSetVisibleToViewer(content, material.getOwnerSubject(), subject))
      .toList();

    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      questionSets.forEach(content -> learningQuestionProgressService.markStarted(materialId, content.getId(), subject, groups));
    }

    return questionSets.stream().map(learningContentGenerationService::toResponse).toList();
  }

  @Transactional
  public LearningQuestionAttemptResponse submitAttempt(String materialId, String subject, Collection<String> groups, LearningQuestionAttemptRequest request) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    LearningGeneratedContent generation = learningGeneratedContentRepository.findById(request.generationId())
      .filter(content -> Objects.equals(content.getMaterialId(), materialId))
      .filter(content -> content.getGenerationType() == LearningContentGenerationType.QUESTION_SET)
      .orElseThrow(() -> new LearningMaterialNotFoundException(request.generationId()));

    if (!learningContentGenerationService.canAttemptQuestionSet(generation, material.getOwnerSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    LearningQuestionSetPayload questionSet = readQuestionSet(generation.getStructuredContent());
    List<LearningQuestionAnswerRequest> answers = request.answers() == null ? List.of() : request.answers();
    Map<Integer, Set<Integer>> submittedAnswers = submittedAnswerSets(answers, questionSet.questions().size());
    int score = 0;
    for (int questionIndex = 0; questionIndex < questionSet.questions().size(); questionIndex++) {
      Set<Integer> selectedOptionIndexes = submittedAnswers.getOrDefault(questionIndex, Set.of());
      Set<Integer> correctOptionIndexes = new HashSet<>(questionSet.questions().get(questionIndex).resolvedCorrectOptionIndexes());
      if (selectedOptionIndexes.equals(correctOptionIndexes)) {
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
    learningQuestionProgressService.markSubmitted(materialId, generation.getId(), subject, saved.getId(), score, questionSet.questions().size());
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

  private Map<Integer, Set<Integer>> submittedAnswerSets(List<LearningQuestionAnswerRequest> answers, int questionCount) {
    Map<Integer, Set<Integer>> submittedAnswers = new LinkedHashMap<>();
    for (LearningQuestionAnswerRequest answer : answers) {
      if (answer.questionIndex() < 0 || answer.questionIndex() >= questionCount) {
        continue;
      }

      Set<Integer> selectedOptionIndexes = answer.selectedOptionIndexes() == null ? Set.of() : new HashSet<>(answer.selectedOptionIndexes());
      submittedAnswers.computeIfAbsent(answer.questionIndex(), ignored -> new HashSet<>()).addAll(selectedOptionIndexes);
    }
    return submittedAnswers;
  }
}
