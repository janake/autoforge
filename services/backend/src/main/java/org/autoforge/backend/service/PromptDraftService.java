package org.autoforge.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import org.autoforge.backend.domain.PromptDraft;
import org.autoforge.backend.domain.PromptDraftMessage;
import org.autoforge.backend.domain.PromptDraftMessageRole;
import org.autoforge.backend.domain.PromptIntent;
import org.autoforge.backend.domain.PromptDraftStatus;
import org.autoforge.backend.dto.AddPromptDraftMessageRequest;
import org.autoforge.backend.dto.CreatePromptDraftRequest;
import org.autoforge.backend.dto.JobResponse;
import org.autoforge.backend.dto.PromptDraftMessageResponse;
import org.autoforge.backend.dto.PromptDraftResponse;
import org.autoforge.backend.jira.CreateJiraIssueRequest;
import org.autoforge.backend.jira.CreateJiraIssueResponse;
import org.autoforge.backend.jira.JiraIssueClient;
import org.autoforge.backend.repository.PromptDraftMessageRepository;
import org.autoforge.backend.repository.PromptDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptDraftService {

  private static final String READY_MESSAGE = "This looks ready for approval.";

  private final PromptDraftRepository promptDraftRepository;
  private final PromptDraftMessageRepository promptDraftMessageRepository;
  private final JiraIssueClient jiraIssueClient;
  private final JobService jobService;
  private final PromptDraftClarifier promptDraftClarifier;

  @Transactional
  public PromptDraftResponse createDraft(CreatePromptDraftRequest request) {
    PromptDraft draft = promptDraftRepository.save(PromptDraft.create(request.prompt().trim()));
    applyIntentClassification(draft, draft.getPrompt());
    promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.USER, draft.getPrompt()));

    PromptDraftClarificationResult clarification = promptDraftClarifier.clarify(new PromptDraftClarificationRequest(draft.getPrompt()));
    if (clarification.readyForApproval()) {
      draft.setStatus(PromptDraftStatus.READY_FOR_APPROVAL);
      draft.setPendingQuestions(null);
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, assistantMessage(clarification)));
    } else {
      draft.setStatus(PromptDraftStatus.CLARIFYING);
      draft.setPendingQuestions(String.join("\n", clarification.questions()));
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, assistantMessage(clarification)));
    }

    return toResponse(promptDraftRepository.save(draft));
  }

  @Transactional(readOnly = true)
  public PromptDraftResponse getDraft(String draftId) {
    return toResponse(loadDraft(draftId));
  }

  @Transactional
  public PromptDraftResponse addMessage(String draftId, AddPromptDraftMessageRequest request) {
    PromptDraft draft = loadDraft(draftId);
    if (draft.getStatus() == PromptDraftStatus.APPROVED) {
      throw new PromptDraftApprovalException("Approved prompt draft cannot be edited: " + draftId);
    }
    promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.USER, request.content().trim()));

    String conversationText = combinedConversationText(draft.getPrompt(), draft.getId());
    applyIntentClassification(draft, conversationText);

    PromptDraftClarificationResult clarification = promptDraftClarifier.clarify(new PromptDraftClarificationRequest(conversationText));
    if (clarification.readyForApproval()) {
      draft.setStatus(PromptDraftStatus.READY_FOR_APPROVAL);
      draft.setPendingQuestions(null);
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, assistantMessage(clarification)));
    } else {
      draft.setStatus(PromptDraftStatus.CLARIFYING);
      draft.setPendingQuestions(String.join("\n", clarification.questions()));
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, assistantMessage(clarification)));
    }

    return toResponse(promptDraftRepository.save(draft));
  }

  @Transactional
  public PromptDraftResponse approveDraft(String draftId, String approvedBy, PromptIntent selectedIntent) {
    PromptDraft draft = loadDraft(draftId);

    if (draft.getStatus() != PromptDraftStatus.READY_FOR_APPROVAL) {
      throw new PromptDraftApprovalException("Prompt draft is not ready for approval: " + draftId);
    }

    String approver = approvedBy == null || approvedBy.isBlank() ? "unknown" : approvedBy.trim();
    PromptIntent finalSelectedIntent = requireSelectedIntent(draftId, selectedIntent);
    draft.approve(approver, Instant.now(), finalSelectedIntent);
    promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, "Approved by %s.".formatted(approver)));

    return toResponse(promptDraftRepository.save(draft));
  }

  @Transactional
  public PromptDraftResponse createJiraTicket(String draftId, String createdBy) {
    PromptDraft draft = loadDraft(draftId);

    if (draft.getJiraIssueKey() != null && !draft.getJiraIssueKey().isBlank()) {
      return toResponse(draft);
    }

    if (draft.getStatus() != PromptDraftStatus.APPROVED) {
      throw new PromptDraftTicketException("Prompt draft is not approved: " + draftId);
    }

    if (draft.getIntent() == PromptIntent.QUESTION) {
      throw new PromptDraftTicketException("Question drafts do not create Jira tickets: " + draftId);
    }

    CreateJiraIssueResponse ticket = jiraIssueClient.createIssue(new CreateJiraIssueRequest(
      "AUTO",
      toJiraIssueType(draft.getIntent()),
      summaryForJiraIssue(draft.getPrompt()),
      descriptionForJiraIssue(draft),
      List.of("prompt-draft", "prompt-flow", "approved-by-" + safeLabel(createdBy))
    ));

    draft.markTicketCreated(ticket.issueKey(), ticket.issueUrl());
    promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, "Jira ticket created: %s".formatted(ticket.issueKey())));
    return toResponse(promptDraftRepository.save(draft));
  }

  @Transactional
  public JobResponse createImplementationJob(String draftId) {
    PromptDraft draft = loadDraft(draftId);

    if (draft.getStatus() != PromptDraftStatus.TICKET_CREATED) {
      throw new PromptDraftTicketException("Prompt draft does not have a Jira ticket yet: " + draftId);
    }

    if (draft.getJiraIssueKey() == null || draft.getJiraIssueKey().isBlank()) {
      throw new PromptDraftTicketException("Prompt draft does not have a Jira issue key: " + draftId);
    }

    return jobService.createJobFromPromptDraft(draft);
  }

  private PromptDraft loadDraft(String draftId) {
    return promptDraftRepository.findById(draftId).orElseThrow(() -> new PromptDraftNotFoundException(draftId));
  }

  private String combinedConversationText(String prompt, String draftId) {
    List<PromptDraftMessage> messages = promptDraftMessageRepository.findByDraftIdOrderByCreatedAtAsc(draftId);
    StringJoiner joiner = new StringJoiner(" ");
    joiner.add(prompt);
    for (PromptDraftMessage message : messages) {
      if (message.getRole() == PromptDraftMessageRole.USER) {
        joiner.add(message.getContent());
      }
    }
    return joiner.toString();
  }

  private String assistantMessage(PromptDraftClarificationResult clarification) {
    if (clarification.message() != null && !clarification.message().isBlank()) {
      return clarification.message().trim();
    }
    if (clarification.readyForApproval()) {
      return READY_MESSAGE;
    }
    return String.join("\n", clarification.questions());
  }

  private void applyIntentClassification(PromptDraft draft, String text) {
    PromptIntentClassification classification = classifyIntent(text);
    draft.setIntent(classification.intent());
    draft.setIntentConfidence(classification.confidence());
    draft.setIntentReason(classification.reason());
    promptDraftRepository.save(draft);
  }

  private PromptIntentClassification classifyIntent(String text) {
    String normalized = Objects.requireNonNullElse(text, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();

    if (normalized.isBlank()) {
      return new PromptIntentClassification(PromptIntent.TASK, 0.25, "Empty prompt defaults to task");
    }

    if (normalized.contains("?") || normalized.matches(".*\\b(how|what|why|when|where|which|can you|could you|should i)\\b.*")) {
      return new PromptIntentClassification(PromptIntent.QUESTION, 0.96, "Interrogative wording or question mark detected");
    }

    if (normalized.matches(".*\\b(bug|error|fail|failure|exception|broken|doesn't work|does not work|regression|fix)\\b.*")) {
      return new PromptIntentClassification(PromptIntent.BUG, 0.88, "Bug/failure keywords detected");
    }

    if (normalized.matches(".*\\b(epic|large initiative|multi-step initiative|program of work)\\b.*")) {
      return new PromptIntentClassification(PromptIntent.EPIC, 0.84, "Epic-sized scope keyword detected");
    }

    if (normalized.matches(".*\\b(feature|implement|add|build|create|support|enhance|introduce)\\b.*")) {
      return new PromptIntentClassification(PromptIntent.FEATURE, 0.82, "Feature/implementation keywords detected");
    }

    return new PromptIntentClassification(PromptIntent.TASK, 0.61, "Defaulted to task after no stronger intent signal");
  }

  private PromptDraftResponse toResponse(PromptDraft draft) {
    List<PromptDraftMessageResponse> messages = promptDraftMessageRepository.findByDraftIdOrderByCreatedAtAsc(draft.getId()).stream()
      .map(message -> new PromptDraftMessageResponse(message.getRole().name(), message.getContent(), message.getCreatedAt()))
      .toList();

    List<String> pendingQuestions = draft.getPendingQuestions() == null || draft.getPendingQuestions().isBlank()
      ? List.of()
      : List.of(draft.getPendingQuestions().split("\\n"));

    return new PromptDraftResponse(
      draft.getId(),
      draft.getPrompt(),
      draft.getStatus().name(),
      draft.getIntent() == null ? null : draft.getIntent().name(),
      draft.getIntentConfidence(),
      draft.getIntentReason(),
      draft.getSelectedIntent() == null ? null : draft.getSelectedIntent().name(),
      draft.getStatus() == PromptDraftStatus.READY_FOR_APPROVAL,
      draft.getApprovedBy(),
      draft.getApprovedAt(),
      draft.getJiraIssueKey(),
      draft.getJiraIssueUrl(),
      pendingQuestions,
      messages,
      draft.getCreatedAt(),
      draft.getUpdatedAt()
    );
  }

  private String toJiraIssueType(PromptIntent intent) {
    if (intent == null) {
      return "Task";
    }

    return switch (intent) {
      case BUG -> "Bug";
      case FEATURE -> "Story";
      case EPIC -> "Epic";
      case TASK -> "Task";
      case QUESTION -> "Task";
    };
  }

  private String summaryForJiraIssue(String prompt) {
    String cleaned = prompt == null ? "" : prompt.replaceAll("\\s+", " ").trim();
    if (cleaned.length() <= 120) {
      return cleaned;
    }
    return cleaned.substring(0, 117) + "...";
  }

  private String descriptionForJiraIssue(PromptDraft draft) {
    StringBuilder builder = new StringBuilder();
    builder.append("Source prompt:\n").append(draft.getPrompt()).append("\n\n");
    if (draft.getApprovedBy() != null) {
      builder.append("Approved by: ").append(draft.getApprovedBy()).append("\n");
    }
    if (draft.getIntent() != null) {
      builder.append("Intent: ").append(draft.getIntent().name()).append("\n");
    }
    if (draft.getIntentReason() != null) {
      builder.append("Intent reason: ").append(draft.getIntentReason()).append("\n");
    }
    if (draft.getPendingQuestions() != null && !draft.getPendingQuestions().isBlank()) {
      builder.append("Pending questions:\n").append(draft.getPendingQuestions()).append("\n");
    }
    return builder.toString().trim();
  }

  private String safeLabel(String value) {
    if (value == null || value.isBlank()) {
      return "unknown";
    }
    return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
  }

  private PromptIntent requireSelectedIntent(String draftId, PromptIntent selectedIntent) {
    if (selectedIntent == null) {
      throw new PromptDraftApprovalException("Prompt draft approval requires a selected intent: " + draftId);
    }

    if (selectedIntent == PromptIntent.QUESTION) {
      throw new PromptDraftApprovalException("Prompt draft approval cannot select QUESTION: " + draftId);
    }

    return selectedIntent;
  }
}
