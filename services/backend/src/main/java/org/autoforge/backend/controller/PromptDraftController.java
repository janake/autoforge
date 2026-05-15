package org.autoforge.backend.controller;

import jakarta.validation.Valid;
import org.autoforge.backend.dto.ApprovePromptDraftRequest;
import org.autoforge.backend.dto.AddPromptDraftMessageRequest;
import org.autoforge.backend.dto.CreatePromptDraftRequest;
import org.autoforge.backend.dto.JobResponse;
import org.autoforge.backend.dto.PromptDraftResponse;
import org.autoforge.backend.service.PromptDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prompt-drafts")
public class PromptDraftController {

  private final PromptDraftService promptDraftService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PromptDraftResponse createDraft(@Valid @RequestBody CreatePromptDraftRequest request) {
    return promptDraftService.createDraft(request);
  }

  @GetMapping("/{draftId}")
  public PromptDraftResponse getDraft(@PathVariable String draftId) {
    return promptDraftService.getDraft(draftId);
  }

  @PostMapping("/{draftId}/messages")
  public PromptDraftResponse addMessage(
    @PathVariable String draftId,
    @Valid @RequestBody AddPromptDraftMessageRequest request
  ) {
    return promptDraftService.addMessage(draftId, request);
  }

  @PostMapping("/{draftId}/approve")
  public PromptDraftResponse approveDraft(
    @PathVariable String draftId,
    @Valid @RequestBody ApprovePromptDraftRequest request,
    Authentication authentication
  ) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new IllegalStateException("Expected JWT authentication");
    }

    Jwt jwt = token.getToken();
    String approvedBy = jwt.getClaimAsString("preferred_username");
    return promptDraftService.approveDraft(draftId, approvedBy, request.selectedIntent());
  }

  @PostMapping("/{draftId}/jira-ticket")
  public PromptDraftResponse createJiraTicket(
    @PathVariable String draftId,
    Authentication authentication
  ) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new IllegalStateException("Expected JWT authentication");
    }

    Jwt jwt = token.getToken();
    String createdBy = jwt.getClaimAsString("preferred_username");
    return promptDraftService.createJiraTicket(draftId, createdBy);
  }

  @PostMapping("/{draftId}/job")
  public JobResponse createImplementationJob(@PathVariable String draftId) {
    return promptDraftService.createImplementationJob(draftId);
  }
}
