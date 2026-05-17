package org.autoforge.backend.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.dto.LearningMaterialAssignmentRequest;
import org.autoforge.backend.dto.LearningMaterialResponse;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningMaterialService {

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;

  @Transactional(readOnly = true)
  public List<LearningMaterialResponse> listAccessibleMaterials(String subject, Collection<String> groups) {
    Set<String> normalizedGroups = normalizeGroups(groups);
    Set<String> materialIds = new LinkedHashSet<>();

    learningMaterialRepository.findByOwnerSubject(subject).forEach(material -> materialIds.add(material.getId()));

    materialIds.addAll(learningMaterialAssignmentRepository
      .findByTargetTypeAndTargetIdentifierIn(LearningAssignmentTargetType.STUDENT, List.of(subject))
      .stream()
      .map(LearningMaterialAssignment::getMaterialId)
      .collect(Collectors.toSet()));

    if (!normalizedGroups.isEmpty()) {
      materialIds.addAll(learningMaterialAssignmentRepository
        .findByTargetTypeAndTargetIdentifierIn(LearningAssignmentTargetType.GROUP, normalizedGroups)
        .stream()
        .map(LearningMaterialAssignment::getMaterialId)
        .collect(Collectors.toSet()));
    }

    return learningMaterialRepository.findAllById(materialIds).stream()
      .sorted(Comparator.comparing(LearningMaterial::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
      .map(material -> toResponse(material, subject))
      .toList();
  }

  @Transactional(readOnly = true)
  public LearningMaterialResponse getMaterial(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadMaterial(materialId);
    Set<String> normalizedGroups = normalizeGroups(groups);

    if (!canAccess(material, subject, normalizedGroups)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    return toResponse(material, subject);
  }

  @Transactional
  public LearningMaterialResponse replaceAssignments(String materialId, String subject, LearningMaterialAssignmentRequest request) {
    LearningMaterial material = loadMaterial(materialId);
    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    learningMaterialAssignmentRepository.deleteByMaterialId(materialId);

    List<String> studentSubjects = normalizedDistinctSubjects(request.studentSubjects());
    List<String> groupNames = normalizedDistinctGroups(request.groupNames());

    List<LearningMaterialAssignment> assignments = new ArrayList<>();
    studentSubjects.forEach(student -> assignments.add(LearningMaterialAssignment.create(materialId, LearningAssignmentTargetType.STUDENT, student)));
    groupNames.forEach(group -> assignments.add(LearningMaterialAssignment.create(materialId, LearningAssignmentTargetType.GROUP, group)));
    learningMaterialAssignmentRepository.saveAll(assignments);

    return toResponse(material, subject);
  }

  @Transactional
  public LearningMaterial ensureMaterialExists(String ownerSubject, String title, String description) {
    return learningMaterialRepository.save(LearningMaterial.create(ownerSubject, title, description));
  }

  private LearningMaterial loadMaterial(String materialId) {
    return learningMaterialRepository.findById(materialId).orElseThrow(() -> new LearningMaterialNotFoundException(materialId));
  }

  private boolean canAccess(LearningMaterial material, String subject, Set<String> groups) {
    if (Objects.equals(material.getOwnerSubject(), subject)) {
      return true;
    }

    List<LearningMaterialAssignment> assignments = learningMaterialAssignmentRepository.findByMaterialId(material.getId());
    for (LearningMaterialAssignment assignment : assignments) {
      if (assignment.getTargetType() == LearningAssignmentTargetType.STUDENT && Objects.equals(assignment.getTargetIdentifier(), subject)) {
        return true;
      }
      if (assignment.getTargetType() == LearningAssignmentTargetType.GROUP && groups.contains(assignment.getTargetIdentifier())) {
        return true;
      }
    }

    return false;
  }

  private LearningMaterialResponse toResponse(LearningMaterial material, String subject) {
    List<LearningMaterialAssignment> assignments = learningMaterialAssignmentRepository.findByMaterialId(material.getId());
    List<String> studentSubjects = assignments.stream()
      .filter(assignment -> assignment.getTargetType() == LearningAssignmentTargetType.STUDENT)
      .map(LearningMaterialAssignment::getTargetIdentifier)
      .sorted()
      .toList();
    List<String> groupNames = assignments.stream()
      .filter(assignment -> assignment.getTargetType() == LearningAssignmentTargetType.GROUP)
      .map(LearningMaterialAssignment::getTargetIdentifier)
      .sorted()
      .toList();

    return new LearningMaterialResponse(
      material.getId(),
      material.getTitle(),
      material.getDescription(),
      material.getOwnerSubject(),
      studentSubjects,
      groupNames,
      Objects.equals(material.getOwnerSubject(), subject),
      material.getCreatedAt(),
      material.getUpdatedAt()
    );
  }

  private static List<String> normalizedDistinctSubjects(Collection<String> values) {
    return values == null ? List.of() : values.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(value -> !value.isBlank())
      .distinct()
      .toList();
  }

  private static List<String> normalizedDistinctGroups(Collection<String> values) {
    return values == null ? List.of() : values.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(value -> !value.isBlank())
      .map(LearningMaterialService::normalizeGroupName)
      .distinct()
      .toList();
  }

  private static Set<String> normalizeGroups(Collection<String> groups) {
    return groups == null ? Set.of() : groups.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(value -> !value.isBlank())
      .map(LearningMaterialService::normalizeGroupName)
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String normalizeGroupName(String group) {
    String normalized = group.trim();
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    if (normalized.contains("/")) {
      normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
    }
    return normalized;
  }
}
