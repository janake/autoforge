package org.autoforge.backend.controller;

import jakarta.validation.Valid;
import org.autoforge.backend.dto.AddPromptDraftMessageRequest;
import org.autoforge.backend.dto.CreatePromptDraftRequest;
import org.autoforge.backend.dto.PromptDraftResponse;
import org.autoforge.backend.service.PromptDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
}
