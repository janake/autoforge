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
  "autoforge.mvp.opencode.model=autoforge-openrouter/deepseek/deepseek-v4-flash:free"
})
@AutoConfigureMockMvc
class PromptDraftFallbackMessageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void showsWhyRealAiIsUnavailableWhenOpencodePasswordIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/prompt-drafts")
        .with(jwt().jwt(token -> token.claim("groups", List.of("developer"))).authorities(new SimpleGrantedAuthority("ROLE_developer")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "prompt": "Audit the workflow"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Real AI is unavailable because OPENCODE_SERVER_PASSWORD is not configured")))
      .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Which repository should this apply to?"))));
  }
}
