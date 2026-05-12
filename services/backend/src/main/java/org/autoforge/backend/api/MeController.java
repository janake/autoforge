package org.autoforge.backend.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MeController {

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new IllegalStateException("Expected JWT authentication");
    }

    Jwt jwt = token.getToken();

    return new MeResponse(
      jwt.getSubject(),
      jwt.getClaimAsString("preferred_username"),
      jwt.getClaimAsString("email"),
      extractRoles(jwt),
      Map.of(
        "issuer", jwt.getClaimAsString("iss"),
        "audience", String.valueOf(jwt.getAudience()),
        "authorizedParty", String.valueOf(jwt.getClaimAsString("azp"))
      )
    );
  }

  private List<String> extractRoles(Jwt jwt) {
    Set<String> roles = new TreeSet<>();

    Object realmAccess = jwt.getClaims().get("realm_access");
    if (realmAccess instanceof Map<?, ?> access && access.get("roles") instanceof List<?> realmRoles) {
      roles.addAll(realmRoles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .collect(Collectors.toSet()));
    }

    Object resourceAccess = jwt.getClaims().get("resource_access");
    if (resourceAccess instanceof Map<?, ?> access) {
      access.values().forEach(clientAccess -> {
        if (clientAccess instanceof Map<?, ?> client && client.get("roles") instanceof List<?> clientRoles) {
          roles.addAll(clientRoles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .collect(Collectors.toSet()));
        }
      });
    }

    return roles.stream().sorted().toList();
  }
}
