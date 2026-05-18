package org.autoforge.backend.controller;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.autoforge.backend.dto.LearningImageAssetResponse;
import org.autoforge.backend.service.LearningImageAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning/image-assets")
public class LearningImageAssetController {

  private final LearningImageAssetService learningImageAssetService;

  @GetMapping("/{assetId}/proxy")
  public ResponseEntity<Void> proxyImageAsset(@PathVariable String assetId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningImageAssetService.resolveAssetProxy(assetId, context.subject(), context.groups());
  }

  @GetMapping("/material/{materialId}")
  public List<LearningImageAssetResponse> listMaterialAssets(@PathVariable String materialId, Authentication authentication) {
    UserContext context = currentUser(authentication);
    return learningImageAssetService.listMaterialAssets(materialId, context.subject(), context.groups());
  }

  private UserContext currentUser(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new IllegalStateException("Expected JWT authentication");
    }

    Jwt jwt = token.getToken();
    return new UserContext(jwt.getSubject(), extractGroups(jwt));
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
      .map(LearningImageAssetController::normalizeGroupName)
      .filter(group -> !group.isBlank())
      .collect(Collectors.toSet());
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

  private record UserContext(String subject, Set<String> groups) {
  }
}
