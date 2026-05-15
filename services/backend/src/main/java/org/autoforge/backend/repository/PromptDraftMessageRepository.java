package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.PromptDraftMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptDraftMessageRepository extends JpaRepository<PromptDraftMessage, String> {

  List<PromptDraftMessage> findByDraftIdOrderByCreatedAtAsc(String draftId);
}
