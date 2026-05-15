package org.autoforge.backend.repository;

import org.autoforge.backend.domain.PromptDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptDraftRepository extends JpaRepository<PromptDraft, String> {
}
