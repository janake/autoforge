package org.autoforge.backend.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.autoforge.backend.domain.LearningAssignmentAudit;
import org.autoforge.backend.domain.LearningAssignmentAuditAction;
import org.autoforge.backend.domain.LearningAssignmentAuditTargetType;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.dto.LearningAssignmentAuditResponse;
import org.autoforge.backend.repository.LearningAssignmentAuditRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningAssignmentAuditService {

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;
  private final LearningAssignmentAuditRepository learningAssignmentAuditRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void recordAssignmentChanges(String materialId, String actorSubject, Collection<LearningMaterialAssignment> previousAssignments, Collection<LearningMaterialAssignment> nextAssignments) {
    Set<String> previousKeys = keyedAssignments(previousAssignments);
    Set<String> nextKeys = keyedAssignments(nextAssignments);

    if (!Objects.equals(previousKeys, nextKeys)) {
      learningAssignmentAuditRepository.save(buildAudit(materialId, actorSubject, LearningAssignmentAuditTargetType.MATERIAL, materialId, LearningAssignmentAuditAction.UPDATE, Map.of(
        "previousCount", previousAssignments == null ? 0 : previousAssignments.size(),
        "nextCount", nextAssignments == null ? 0 : nextAssignments.size(),
        "addedCount", countAdded(previousKeys, nextKeys),
        "removedCount", countRemoved(previousKeys, nextKeys)
      )));
    }

    Map<String, LearningMaterialAssignment> previousByKey = mapAssignments(previousAssignments);
    Map<String, LearningMaterialAssignment> nextByKey = mapAssignments(nextAssignments);

    for (String key : diffKeys(previousKeys, nextKeys)) {
      LearningMaterialAssignment added = nextByKey.get(key);
      if (added != null) {
        learningAssignmentAuditRepository.save(buildAudit(materialId, actorSubject, toAuditTargetType(added.getTargetType()), added.getTargetIdentifier(), LearningAssignmentAuditAction.CREATE, Map.of(
          "materialId", materialId,
          "targetType", added.getTargetType().name(),
          "targetIdentifier", added.getTargetIdentifier()
        )));
        continue;
      }

      LearningMaterialAssignment removed = previousByKey.get(key);
      if (removed != null) {
        learningAssignmentAuditRepository.save(buildAudit(materialId, actorSubject, toAuditTargetType(removed.getTargetType()), removed.getTargetIdentifier(), LearningAssignmentAuditAction.DELETE, Map.of(
          "materialId", materialId,
          "targetType", removed.getTargetType().name(),
          "targetIdentifier", removed.getTargetIdentifier()
        )));
      }
    }
  }

  @Transactional(readOnly = true)
  public List<LearningAssignmentAuditResponse> listMaterialAudits(String materialId, String subject, boolean canManageAssignments) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, canManageAssignments);
    return learningAssignmentAuditRepository.findByMaterialIdOrderByCreatedAtDesc(material.getId()).stream()
      .map(audit -> new LearningAssignmentAuditResponse(
        audit.getAuditId(),
        audit.getMaterialId(),
        audit.getActorSubject(),
        audit.getTargetType(),
        audit.getTargetIdentifier(),
        audit.getAction(),
        audit.getDetailsJson(),
        audit.getCreatedAt()
      ))
      .toList();
  }

  private LearningMaterial loadAccessibleMaterial(String materialId, String subject, boolean canManageAssignments) {
    LearningMaterial material = learningMaterialRepository.findById(materialId).orElseThrow(() -> new LearningMaterialNotFoundException(materialId));
    if (!Objects.equals(material.getOwnerSubject(), subject) && !canManageAssignments) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }
    return material;
  }

  private LearningAssignmentAudit buildAudit(String materialId, String actorSubject, LearningAssignmentAuditTargetType targetType, String targetIdentifier, LearningAssignmentAuditAction action, Object details) {
    return LearningAssignmentAudit.create(materialId, actorSubject, targetType, targetIdentifier, action, serializeDetails(materialId, details));
  }

  private String serializeDetails(String materialId, Object details) {
    try {
      return objectMapper.writeValueAsString(details == null ? Map.of() : details);
    } catch (JsonProcessingException exception) {
      log.warn("Failed to serialize assignment audit details for material {}", materialId, exception);
      return "{}";
    }
  }

  private static Map<String, LearningMaterialAssignment> mapAssignments(Collection<LearningMaterialAssignment> assignments) {
    if (assignments == null) {
      return Map.of();
    }

    Map<String, LearningMaterialAssignment> mapped = new LinkedHashMap<>();
    for (LearningMaterialAssignment assignment : assignments) {
      mapped.put(assignmentKey(assignment.getTargetType(), assignment.getTargetIdentifier()), assignment);
    }
    return mapped;
  }

  private static Set<String> keyedAssignments(Collection<LearningMaterialAssignment> assignments) {
    return mapAssignments(assignments).keySet();
  }

  private static List<String> diffKeys(Set<String> previousKeys, Set<String> nextKeys) {
    return java.util.stream.Stream.concat(previousKeys.stream(), nextKeys.stream())
      .distinct()
      .filter(key -> previousKeys.contains(key) != nextKeys.contains(key))
      .toList();
  }

  private static int countAdded(Set<String> previousKeys, Set<String> nextKeys) {
    return (int) nextKeys.stream().filter(key -> !previousKeys.contains(key)).count();
  }

  private static int countRemoved(Set<String> previousKeys, Set<String> nextKeys) {
    return (int) previousKeys.stream().filter(key -> !nextKeys.contains(key)).count();
  }

  private static String assignmentKey(LearningAssignmentTargetType targetType, String targetIdentifier) {
    return targetType.name() + ":" + targetIdentifier;
  }

  private static LearningAssignmentAuditTargetType toAuditTargetType(LearningAssignmentTargetType targetType) {
    return switch (targetType) {
      case STUDENT -> LearningAssignmentAuditTargetType.STUDENT;
      case GROUP -> LearningAssignmentAuditTargetType.GROUP;
    };
  }
}
