package org.autoforge.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void returnsCurrentUserProfileFromJwtClaims() throws Exception {
    mockMvc.perform(get("/api/v1/me").with(jwt().jwt(token -> token
        .subject("user-123")
        .claim("preferred_username", "alex")
        .claim("email", "alex@example.com")
        .claim("iss", "https://kc.prodet.org/realms/<keycloak-realm>")
        .claim("aud", List.of("autoforge-web"))
        .claim("azp", "autoforge-web")
        .claim("groups", List.of("/developer", "/platform"))
        .claim("realm_access", Map.of("roles", List.of("admin", "builder")))
        .claim("resource_access", Map.of("autoforge-web", Map.of("roles", List.of("viewer")))))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.subject").value("user-123"))
      .andExpect(jsonPath("$.username").value("alex"))
      .andExpect(jsonPath("$.email").value("alex@example.com"))
      .andExpect(jsonPath("$.roles[0]").value("admin"))
      .andExpect(jsonPath("$.roles[1]").value("builder"))
      .andExpect(jsonPath("$.roles[2]").value("viewer"))
      .andExpect(jsonPath("$.groups[0]").value("developer"))
      .andExpect(jsonPath("$.groups[1]").value("platform"))
      .andExpect(jsonPath("$.claims.issuer").value("https://kc.prodet.org/realms/<keycloak-realm>"))
      .andExpect(jsonPath("$.claims.authorizedParty").value("autoforge-web"));
  }
}
