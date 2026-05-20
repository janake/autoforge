package org.autoforge.backend.controller;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.autoforge.backend.dto.LearningMaterialAssignmentRequest;
import org.autoforge.backend.dto.LearningContentGenerationResponse;
import org.autoforge.backend.dto.LearningQuestionAttemptRequest;
import org.autoforge.backend.dto.LearningQuestionAttemptResponse;
import org.autoforge.backend.dto.LearningQuestionDisputeRequest;
import org.autoforge.backend.dto.LearningQuestionDisputeResponse;
import org.autoforge.backend.dto.LearningQuestionDisputeReviewRequest;
import org.autoforge.backend.dto.LearningAssignmentAuditResponse;
import org.autoforge.backend.dto.LearningQuestionSetSettingsRequest;
import org.autoforge.backend.service.LearningContentGenerationService;
import org.autoforge.backend.dto.LearningIngestionResponse;
import org.autoforge.backend.dto.LearningMaterialResponse;
import org.autoforge.backend.service.LearningMaterialService;
import org.autoforge.backend.service.LearningAssignmentAuditService;
import org.autoforge.backend.service.LearningIngestionService;
import org.autoforge.backend.service.LearningQuestionAttemptService;
import org.autoforge.backend.service.LearningQuestionDisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning/materials")
public class LearningMaterialController {

  private final LearningMaterialService learningMaterialService;
  private final LearningContentGenerationService learningContentGenerationService;
  private final LearningIngestionService learningIngestionService;
  private final LearningQuestionAttemptService learningQuestionAttemptService;
  private final LearningQuestionDisputeService learningQuestionDisputeService;
  private final LearningAssignmentAuditService learningAssignmentAuditService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public LearningMaterialResponse uploadMaterial(
    @RequestParam(required = false) String title,
    @RequestParam(required = false) String description,
    @RequestParam("file") MultipartFile file,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.uploadMaterial(context.subject(), context.canCreateLearningContent(), file, title, description);
  }

  @PostMapping(value = "/{materialId}/sources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public LearningMaterialResponse addSource(
    @PathVariable String materialId,
    @RequestParam(required = false) String sourceName,
    @RequestParam("file") MultipartFile file,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.addSource(materialId, context.subject(), file, sourceName);
  }

  @DeleteMapping("/{materialId}/sources/{sourceId}")
  public LearningMaterialResponse deleteSource(
    @PathVariable String materialId,
    @PathVariable String sourceId,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.deleteSource(materialId, context.subject(), sourceId);
  }

  @GetMapping("/{materialId}/ingestion")
  public LearningIngestionResponse getIngestion(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningIngestionService.getIngestion(materialId, context.subject());
  }

  @PostMapping("/{materialId}/ingestion")
  public LearningIngestionResponse startIngestion(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningIngestionService.startIngestion(materialId, context.subject());
  }

  @PostMapping("/{materialId}/ingestion/retry")
  public LearningIngestionResponse retryIngestion(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningIngestionService.retryIngestion(materialId, context.subject());
  }

  @GetMapping
  public List<LearningMaterialResponse> listMaterials(Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.listAccessibleMaterials(context.subject(), context.groups(), context.canManageAssignments());
  }

  @GetMapping("/{materialId}")
  public LearningMaterialResponse getMaterial(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.getMaterial(materialId, context.subject(), context.groups(), context.canManageAssignments());
  }

  @GetMapping("/{materialId}/optimized-image")
  public ResponseEntity<byte[]> getOptimizedImage(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.serveOptimizedImage(materialId, context.subject(), context.groups());
  }

  @PostMapping("/{materialId}/questions")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningContentGenerationResponse generateQuestions(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningContentGenerationService.generateQuestions(materialId, context.subject());
  }

  @PostMapping("/{materialId}/summary")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningContentGenerationResponse generateSummary(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningContentGenerationService.generateSummary(materialId, context.subject());
  }

  @GetMapping("/{materialId}/generations")
  public List<LearningContentGenerationResponse> listGenerations(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningContentGenerationService.listGeneratedContent(materialId, context.subject(), context.groups());
  }

  @GetMapping("/{materialId}/question-sets")
  public List<LearningContentGenerationResponse> listQuestionSets(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningQuestionAttemptService.listQuestionSets(materialId, context.subject(), context.groups());
  }

  @PostMapping("/{materialId}/question-sets/{generationId}/publish")
  public LearningContentGenerationResponse publishQuestionSet(
    @PathVariable String materialId,
    @PathVariable String generationId,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningContentGenerationService.publishQuestionSet(materialId, generationId, context.subject());
  }

  @PostMapping("/{materialId}/question-sets/{generationId}/archive")
  public LearningContentGenerationResponse archiveQuestionSet(
    @PathVariable String materialId,
    @PathVariable String generationId,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningContentGenerationService.archiveQuestionSet(materialId, generationId, context.subject());
  }

  @PutMapping("/{materialId}/question-sets/{generationId}/settings")
  public LearningContentGenerationResponse updateQuestionSetSettings(
    @PathVariable String materialId,
    @PathVariable String generationId,
    @RequestBody LearningQuestionSetSettingsRequest request,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningContentGenerationService.updateQuestionSetSettings(materialId, generationId, context.subject(), request);
  }

  @PostMapping("/{materialId}/question-attempts")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningQuestionAttemptResponse submitQuestionAttempt(
    @PathVariable String materialId,
    @RequestBody LearningQuestionAttemptRequest request,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningQuestionAttemptService.submitAttempt(materialId, context.subject(), context.groups(), request);
  }

  @GetMapping("/{materialId}/question-attempts")
  public List<LearningQuestionAttemptResponse> listQuestionAttempts(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningQuestionAttemptService.listAttempts(materialId, context.subject(), context.groups());
  }

  @PostMapping("/{materialId}/question-attempts/{attemptId}/disputes")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningQuestionDisputeResponse createQuestionDispute(
    @PathVariable String materialId,
    @PathVariable String attemptId,
    @RequestBody LearningQuestionDisputeRequest request,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningQuestionDisputeService.createDispute(materialId, context.subject(), context.groups(), attemptId, request);
  }

  @GetMapping("/{materialId}/question-disputes")
  public List<LearningQuestionDisputeResponse> listQuestionDisputes(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningQuestionDisputeService.listDisputes(materialId, context.subject(), context.groups());
  }

  @PostMapping("/{materialId}/question-disputes/{disputeId}/review")
  public LearningQuestionDisputeResponse reviewQuestionDispute(
    @PathVariable String materialId,
    @PathVariable String disputeId,
    @RequestBody LearningQuestionDisputeReviewRequest request,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningQuestionDisputeService.reviewDispute(materialId, context.subject(), context.groups(), disputeId, request);
  }

  @PutMapping("/{materialId}/assignments")
  public LearningMaterialResponse replaceAssignments(
    @PathVariable String materialId,
    @RequestBody LearningMaterialAssignmentRequest request,
    Authentication authentication
  ) {
    UserContext context = currentUser(authentication);
    return learningMaterialService.replaceAssignments(materialId, context.subject(), context.canManageAssignments(), request);
  }

  @GetMapping("/{materialId}/assignment-audit")
  public List<LearningAssignmentAuditResponse> listAssignmentAudit(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningAssignmentAuditService.listMaterialAudits(materialId, context.subject(), context.canManageAssignments());
  }

  private UserContext currentUser(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new IllegalStateException("Expected JWT authentication");
    }

    Jwt jwt = token.getToken();
    Set<String> groups = extractGroups(jwt);
    return new UserContext(jwt.getSubject(), groups, extractRoles(jwt), groups);
  }

  private Set<String> extractRoles(Jwt jwt) {
    Set<String> roles = new java.util.TreeSet<>();

    Object realmAccess = jwt.getClaims().get("realm_access");
    if (realmAccess instanceof java.util.Map<?, ?> access && access.get("roles") instanceof List<?> realmRoles) {
      roles.addAll(realmRoles.stream()
        .filter(Objects::nonNull)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(LearningMaterialController::normalizePermissionName)
        .collect(Collectors.toSet()));
    }

    Object resourceAccess = jwt.getClaims().get("resource_access");
    if (resourceAccess instanceof java.util.Map<?, ?> access) {
      access.values().forEach(clientAccess -> {
        if (clientAccess instanceof java.util.Map<?, ?> client && client.get("roles") instanceof List<?> clientRoles) {
          roles.addAll(clientRoles.stream()
            .filter(Objects::nonNull)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(LearningMaterialController::normalizePermissionName)
            .collect(Collectors.toSet()));
        }
      });
    }

    return roles;
  }

  private Set<String> extractGroups(Jwt jwt) {
    Object groupsClaim = jwt.getClaims().get("groups");
    if (!(groupsClaim instanceof List<?> jwtGroups)) {
      return Set.of();
    }

    return jwtGroups.stream()
      .filter(Objects::nonNull)
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .map(LearningMaterialController::normalizeGroupName)
      .filter(group -> !group.isBlank())
      .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
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

  private static String normalizePermissionName(String value) {
    return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
  }

  private record UserContext(String subject, Set<String> groups, Set<String> roles, Set<String> normalizedGroups) {
    private boolean canCreateLearningContent() {
      return canManageAssignments();
    }

    private boolean canManageAssignments() {
      return roles.contains("admin")
        || roles.contains("teacher")
        || roles.contains("learning_teacher")
        || normalizedGroups.contains("teacher")
        || normalizedGroups.contains("teachers");
    }
  }
}
