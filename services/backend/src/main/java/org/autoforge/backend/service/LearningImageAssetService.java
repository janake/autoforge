package org.autoforge.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningImageAsset;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.dto.LearningImageAssetResponse;
import org.autoforge.backend.repository.LearningImageAssetRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningImageAssetService {

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;
  private final LearningImageAssetRepository learningImageAssetRepository;

  @Transactional(readOnly = true)
  public List<LearningImageAssetResponse> listMaterialAssets(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadAccessibleMaterial(materialId, subject, groups);
    return learningImageAssetRepository.findByMaterialIdOrderByCreatedAtAsc(material.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Void> resolveAssetProxy(String assetId, String subject, Collection<String> groups) {
    LearningImageAsset asset = learningImageAssetRepository.findById(assetId)
      .orElseThrow(() -> new LearningMaterialNotFoundException(assetId));
    LearningMaterial material = loadAccessibleMaterial(asset.getMaterialId(), subject, groups);
    if (!Objects.equals(asset.getMaterialId(), material.getId())) {
      throw new LearningMaterialAccessDeniedException(assetId);
    }

    return ResponseEntity.status(302)
      .header(HttpHeaders.LOCATION, asset.getStorageObjectUri())
      .build();
  }

  private LearningMaterial loadAccessibleMaterial(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = learningMaterialRepository.findById(materialId)
      .orElseThrow(() -> new LearningMaterialNotFoundException(materialId));
    if (!canAccess(material, subject, groups)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }
    return material;
  }

  private boolean canAccess(LearningMaterial material, String subject, Collection<String> groups) {
    if (Objects.equals(material.getOwnerSubject(), subject)) {
      return true;
    }

    List<LearningMaterialAssignment> assignments = learningMaterialAssignmentRepository.findByMaterialId(material.getId());
    for (LearningMaterialAssignment assignment : assignments) {
      if (assignment.getTargetType() == LearningAssignmentTargetType.STUDENT && Objects.equals(assignment.getTargetIdentifier(), subject)) {
        return true;
      }
      if (assignment.getTargetType() == LearningAssignmentTargetType.GROUP && groups != null && groups.contains(assignment.getTargetIdentifier())) {
        return true;
      }
    }

    return false;
  }

  private LearningImageAssetResponse toResponse(LearningImageAsset asset) {
    return new LearningImageAssetResponse(
      asset.getId(),
      asset.getMaterialId(),
      asset.getMimeType(),
      asset.getSizeBytes(),
      asset.getContentHash(),
      asset.getAltText(),
      "/api/v1/learning/image-assets/%s/proxy".formatted(asset.getId()),
      asset.getCreatedAt()
    );
  }
}
