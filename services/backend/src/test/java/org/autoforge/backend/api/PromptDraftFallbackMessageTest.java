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
  "autoforge.mvp.opencode.server-url=http://opencode:4096",
  "autoforge.mvp.opencode.username=opencode",
  "autoforge.mvp.opencode.password=",
  "autoforge.mvp.opencode.model=autoforge-openrouter/google/gemma-4-26b-a4b-it:free"
})
@AutoConfigureMockMvc
class PromptDraftFallbackMessageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void returnsServiceUnavailableWhenOpencodePasswordIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(jwt().jwt(token -> token.claim("groups", List.of("developer"))).authorities(new SimpleGrantedAuthority("ROLE_developer")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Prompt draft AI is unavailable because OPENCODE_SERVER_PASSWORD is not configured")));
  }
}
