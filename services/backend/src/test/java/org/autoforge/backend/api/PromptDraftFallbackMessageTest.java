package org.autoforge.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
  "autoforge.mvp.provider-proxy.base-url=",
  "autoforge.mvp.provider-proxy.model=google/gemma-4-26b-a4b-it:free"
})
@AutoConfigureMockMvc
class PromptDraftFallbackMessageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void returnsServiceUnavailableWhenProviderProxyBaseUrlIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(jwt().jwt(token -> token.claim("groups", List.of("developer"))).authorities(new SimpleGrantedAuthority("ROLE_developer")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("AI_PROVIDER_PROXY_BASE_URL is not configured")));
  }
}
