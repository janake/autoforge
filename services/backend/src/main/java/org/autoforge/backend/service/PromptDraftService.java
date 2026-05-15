package org.autoforge.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.autoforge.backend.domain.PromptDraft;
import org.autoforge.backend.domain.PromptDraftMessage;
import org.autoforge.backend.domain.PromptDraftMessageRole;
import org.autoforge.backend.domain.PromptDraftStatus;
import org.autoforge.backend.dto.AddPromptDraftMessageRequest;
import org.autoforge.backend.dto.CreatePromptDraftRequest;
import org.autoforge.backend.dto.PromptDraftMessageResponse;
import org.autoforge.backend.dto.PromptDraftResponse;
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

  @Transactional
  public PromptDraftResponse createDraft(CreatePromptDraftRequest request) {
    PromptDraft draft = promptDraftRepository.save(PromptDraft.create(request.prompt().trim()));
    promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.USER, draft.getPrompt()));

    List<String> questions = clarificationQuestions(draft.getPrompt());
    if (questions.isEmpty()) {
      draft.setStatus(PromptDraftStatus.READY_FOR_APPROVAL);
      draft.setPendingQuestions(null);
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, READY_MESSAGE));
    } else {
      draft.setStatus(PromptDraftStatus.CLARIFYING);
      draft.setPendingQuestions(String.join("\n", questions));
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, String.join("\n", questions)));
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
    promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.USER, request.content().trim()));

    List<String> questions = clarificationQuestions(combinedConversationText(draft.getPrompt(), draft.getId()));
    if (questions.isEmpty()) {
      draft.setStatus(PromptDraftStatus.READY_FOR_APPROVAL);
      draft.setPendingQuestions(null);
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, READY_MESSAGE));
    } else {
      draft.setStatus(PromptDraftStatus.CLARIFYING);
      draft.setPendingQuestions(String.join("\n", questions));
      promptDraftMessageRepository.save(PromptDraftMessage.create(draft.getId(), PromptDraftMessageRole.ASSISTANT, String.join("\n", questions)));
    }

    return toResponse(promptDraftRepository.save(draft));
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

  private List<String> clarificationQuestions(String text) {
    String normalized = Objects.requireNonNullElse(text, "").toLowerCase(Locale.ROOT);
    List<String> questions = new ArrayList<>();

    if (!normalized.matches(".*(?:\\b[a-z0-9_.-]+/[a-z0-9_.-]+\\b|\\brepository\\b|\\brepo\\b).*")) {
      questions.add("Which repository should this apply to?");
    }

    if (!normalized.matches(".*(?:\\bacceptance\\b|\\bexpected\\b|\\bdone\\b|\\bshould\\b).*")) {
      questions.add("What does success look like, and what acceptance criteria should we use?");
    }

    if (normalized.replaceAll("\\s+", " ").trim().length() < 40) {
      questions.add("Can you give a bit more detail about the intended change?");
    }

    return questions.stream().distinct().collect(Collectors.toList());
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
      draft.getStatus() == PromptDraftStatus.READY_FOR_APPROVAL,
      pendingQuestions,
      messages,
      draft.getCreatedAt(),
      draft.getUpdatedAt()
    );
  }
}
