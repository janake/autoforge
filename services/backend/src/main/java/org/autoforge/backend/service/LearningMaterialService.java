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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningMaterialSource;
import org.autoforge.backend.dto.LearningMaterialAssignmentRequest;
import org.autoforge.backend.dto.LearningImageAssetResponse;
import org.autoforge.backend.dto.LearningMaterialResponse;
import org.autoforge.backend.dto.LearningMaterialSourceResponse;
import org.autoforge.backend.dto.LearningQuestionProgressResponse;
import org.autoforge.backend.repository.LearningImageAssetRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningMaterialSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LearningMaterialService {

  private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024 * 1024;
  private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
    "application/pdf",
    "text/plain",
    "text/markdown",
    "application/markdown",
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
    "image/bmp"
  );
  private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "txt", "md", "markdown", "jpg", "jpeg", "png", "gif", "webp", "bmp");

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;
  private final LearningMaterialSourceRepository learningMaterialSourceRepository;
  private final LearningImageAssetRepository learningImageAssetRepository;
  private final LearningMaterialObjectStorageService learningMaterialObjectStorageService;
  private final LearningQuestionProgressService learningQuestionProgressService;
  private final LearningAssignmentAuditService learningAssignmentAuditService;
  private final LearningImageProcessingService learningImageProcessingService;

  @Transactional
  public LearningMaterialResponse uploadMaterial(String subject, boolean canCreateLearningContent, MultipartFile file, String title, String description) {
    if (!canCreateLearningContent) {
      throw new LearningMaterialAccessDeniedException("learning-material-upload");
    }
    validateUpload(file);

    String originalFilename = normalizeTitle(file.getOriginalFilename());
    String resolvedTitle = firstNonBlank(title, originalFilename);
    LearningMaterialObjectStorageService.OriginalFileStoragePlan storagePlan = learningMaterialObjectStorageService.prepareOriginalFile(subject, file);
    byte[] content;
    try {
      content = file.getBytes();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read uploaded file", exception);
    }

    String contentType = normalizeTitle(file.getContentType());
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      subject,
      resolvedTitle,
      description,
      originalFilename,
      contentType,
      file.getSize(),
      storagePlan.objectKey(),
      storagePlan.objectUri(),
      storagePlan.contentHash(),
      storagePlan.eTag(),
      content
    ));
    saveSource(material.getId(), subject, "PRIMARY_UPLOAD", resolvedTitle, file, storagePlan, content);

    String extension = extractExtension(originalFilename);
    LearningImageProcessingService.ImageProcessResult processed = learningImageProcessingService.process(content, contentType, extension);
    if (processed.optimized() && processed.optimizedContent() != null) {
      material.withOptimizedImage(
        processed.optimizedContent(),
        processed.optimizedContentType(),
        processed.optimizedFileSize(),
        processed.optimizedContentHash()
      );
      learningMaterialRepository.save(material);
    }

    return toResponse(material, subject, Set.of(), canCreateLearningContent);
  }

  @Transactional
  public LearningMaterialResponse addSource(String materialId, String subject, MultipartFile file, String sourceName) {
    LearningMaterial material = loadMaterial(materialId);
    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    validateUpload(file);
    String resolvedSourceName = firstNonBlank(sourceName, firstNonBlank(file.getOriginalFilename(), "source"));
    LearningMaterialObjectStorageService.OriginalFileStoragePlan storagePlan = learningMaterialObjectStorageService.prepareOriginalFile(subject, file);
    byte[] content = readBytes(file);
    saveSource(material.getId(), subject, "ADDITIONAL_UPLOAD", resolvedSourceName, file, storagePlan, content);
    return toResponse(material, subject, Set.of(), true);
  }

  @Transactional
  public LearningMaterialResponse deleteSource(String materialId, String subject, String sourceId) {
    LearningMaterial material = loadMaterial(materialId);
    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    LearningMaterialSource source = learningMaterialSourceRepository.findByIdAndDeletedAtIsNull(sourceId)
      .orElseThrow(() -> new LearningMaterialNotFoundException(sourceId));
    if (!Objects.equals(source.getMaterialId(), materialId)) {
      throw new LearningMaterialNotFoundException(sourceId);
    }

    source.markDeleted();
    learningMaterialSourceRepository.save(source);
    return toResponse(material, subject, Set.of(), true);
  }

  @Transactional(readOnly = true)
  public List<LearningMaterialResponse> listAccessibleMaterials(String subject, Collection<String> groups, boolean canManageAssignments) {
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
      .map(material -> toResponse(material, subject, groups, canManageAssignments))
      .toList();
  }

  @Transactional(readOnly = true)
  public LearningMaterialResponse getMaterial(String materialId, String subject, Collection<String> groups, boolean canManageAssignments) {
    LearningMaterial material = loadMaterial(materialId);
    Set<String> normalizedGroups = normalizeGroups(groups);

    if (!canAccess(material, subject, normalizedGroups)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    return toResponse(material, subject, Set.of(), canManageAssignments);
  }

  @Transactional
  public LearningMaterialResponse replaceAssignments(String materialId, String subject, boolean canManageAssignments, LearningMaterialAssignmentRequest request) {
    LearningMaterial material = loadMaterial(materialId);
    if (!Objects.equals(material.getOwnerSubject(), subject) && !canManageAssignments) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    List<LearningMaterialAssignment> previousAssignments = learningMaterialAssignmentRepository.findByMaterialId(materialId);
    learningMaterialAssignmentRepository.deleteByMaterialId(materialId);

    List<String> studentSubjects = normalizedDistinctSubjects(request.studentSubjects());
    List<String> groupNames = normalizedDistinctGroups(request.groupNames());

    List<LearningMaterialAssignment> assignments = new ArrayList<>();
    studentSubjects.forEach(student -> assignments.add(LearningMaterialAssignment.create(materialId, LearningAssignmentTargetType.STUDENT, student)));
    groupNames.forEach(group -> assignments.add(LearningMaterialAssignment.create(materialId, LearningAssignmentTargetType.GROUP, group)));
    learningMaterialAssignmentRepository.saveAll(assignments);
    learningAssignmentAuditService.recordAssignmentChanges(materialId, subject, previousAssignments, assignments);

    return toResponse(material, subject, Set.of(), canManageAssignments);
  }

  @Transactional
  public LearningMaterial ensureMaterialExists(String ownerSubject, String title, String description) {
    return learningMaterialRepository.save(LearningMaterial.create(ownerSubject, title, description));
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> serveOptimizedImage(String materialId, String subject, Collection<String> groups) {
    LearningMaterial material = loadMaterial(materialId);
    Set<String> normalizedGroups = normalizeGroups(groups);

    if (!canAccess(material, subject, normalizedGroups)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }

    if (material.getOptimizedContent() == null) {
      return ResponseEntity.notFound().build();
    }

    String contentType = material.getOptimizedContentType() != null ? material.getOptimizedContentType() : "image/jpeg";
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_TYPE, contentType)
      .body(material.getOptimizedContent());
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

  private LearningMaterialResponse toResponse(LearningMaterial material, String subject, Collection<String> groups, boolean canManageAssignments) {
    List<LearningMaterialAssignment> assignments = learningMaterialAssignmentRepository.findByMaterialId(material.getId());
    List<LearningMaterialSourceResponse> sources = learningMaterialSourceRepository.findByMaterialIdAndDeletedAtIsNullOrderByCreatedAtAsc(material.getId()).stream()
      .map(source -> new LearningMaterialSourceResponse(
        source.getId(),
        source.getMaterialId(),
        source.getSourceType(),
        source.getSourceName(),
        source.getOriginalFilename(),
        source.getContentType(),
        source.getFileSize(),
        source.getStorageObjectKey(),
        source.getStorageObjectUri(),
        source.getContentHash(),
        source.getContentETag(),
        source.getDeletedAt(),
        source.getCreatedAt(),
        source.getUpdatedAt()
      ))
      .toList();
    List<LearningImageAssetResponse> imageAssets = learningImageAssetRepository.findByMaterialIdOrderByCreatedAtAsc(material.getId()).stream()
      .map(asset -> new LearningImageAssetResponse(
        asset.getId(),
        asset.getMaterialId(),
        asset.getMimeType(),
        asset.getSizeBytes(),
        asset.getContentHash(),
        asset.getAltText(),
        "/api/v1/learning/image-assets/%s/proxy".formatted(asset.getId()),
        asset.getCreatedAt()
      ))
      .toList();
    List<LearningQuestionProgressResponse> progressEntries = learningQuestionProgressService.listProgress(material.getId(), subject, groups, canManageAssignments);
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

    String optimizedImageUrl = material.getOptimizedContent() != null
      ? "/api/v1/learning/materials/%s/optimized-image".formatted(material.getId())
      : null;

    return new LearningMaterialResponse(
      material.getId(),
      material.getTitle(),
      material.getDescription(),
      material.getOriginalFilename(),
      material.getContentType(),
      material.getFileSize(),
      material.getStorageObjectKey(),
      material.getStorageObjectUri(),
      material.getContentHash(),
      material.getContentETag(),
      material.getOwnerSubject(),
      studentSubjects,
      groupNames,
      Objects.equals(material.getOwnerSubject(), subject) || canManageAssignments,
      sources,
      imageAssets,
      progressEntries,
      optimizedImageUrl,
      material.getOptimizedFileSize(),
      material.getOptimizedContentHash(),
      material.getCreatedAt(),
      material.getUpdatedAt()
    );
  }

  private void saveSource(
    String materialId,
    String ownerSubject,
    String sourceType,
    String sourceName,
    MultipartFile file,
    LearningMaterialObjectStorageService.OriginalFileStoragePlan storagePlan,
    byte[] content
  ) {
    learningMaterialSourceRepository.save(LearningMaterialSource.create(
      materialId,
      ownerSubject,
      sourceType,
      sourceName,
      normalizeTitle(file.getOriginalFilename()),
      normalizeTitle(file.getContentType()),
      file.getSize(),
      storagePlan.objectKey(),
      storagePlan.objectUri(),
      storagePlan.contentHash(),
      storagePlan.eTag(),
      content
    ));
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read uploaded file", exception);
    }
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

  private void validateUpload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file must not be empty");
    }
    if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
      throw new IllegalArgumentException("Uploaded file exceeds 10 MB");
    }

    String filename = file.getOriginalFilename();
    String contentType = file.getContentType();
    String extension = extractExtension(filename);
    boolean supportedContentType = contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    boolean supportedExtension = extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));

    if (!supportedContentType && !supportedExtension) {
      throw new IllegalArgumentException("Unsupported learning material format");
    }
  }

  private static String firstNonBlank(String first, String fallback) {
    if (first != null && !first.isBlank()) {
      return first.trim();
    }
    if (fallback != null && !fallback.isBlank()) {
      return fallback.trim();
    }
    throw new IllegalArgumentException("Learning material title is required");
  }

  private static String normalizeTitle(String value) {
    return value == null ? null : value.trim();
  }

  private static String extractExtension(String filename) {
    if (filename == null || filename.isBlank()) {
      return null;
    }
    int lastDot = filename.lastIndexOf('.');
    if (lastDot < 0 || lastDot == filename.length() - 1) {
      return null;
    }
    return filename.substring(lastDot + 1);
  }
}
