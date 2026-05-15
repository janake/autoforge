package org.autoforge.backend.service;

import java.text.Normalizer;
import java.util.Locale;
import org.autoforge.backend.domain.Job;
import org.springframework.stereotype.Service;

@Service
public class BranchNamingService {

  private static final int MAX_SLUG_LENGTH = 48;

  public String branchNameFor(Job job) {
    String branchType = branchTypeFor(job.getPrompt());
    String reference = referenceFor(job.getJiraIssueKey(), job.getId());
    String slug = slugify(job.getPrompt());

    return "%s/%s-%s".formatted(branchType, reference, slug);
  }

  public String branchTypeFor(String prompt) {
    String normalized = normalize(prompt);

    if (normalized.matches(".*\\b(bug|error|fail|failure|exception|broken|does not work|doesn't work|regression|fix)\\b.*")) {
      return "bug";
    }

    if (normalized.matches(".*\\b(feature|implement|add|build|create|support|enhance|introduce|epic)\\b.*")) {
      return "feature";
    }

    return "task";
  }

  public String referenceFor(String jiraIssueKey, String jobId) {
    if (jiraIssueKey != null && !jiraIssueKey.isBlank()) {
      return jiraIssueKey.trim().toUpperCase(Locale.ROOT);
    }

    String compactJobId = jobId == null ? "unknown" : jobId.replaceAll("[^a-zA-Z0-9]", "");
    if (compactJobId.length() > 8) {
      compactJobId = compactJobId.substring(0, 8);
    }
    return "JOB-%s".formatted(compactJobId.isBlank() ? "unknown" : compactJobId);
  }

  public String slugify(String value) {
    String normalized = normalize(value);
    String slug = Normalizer.normalize(normalized, Normalizer.Form.NFD)
      .replaceAll("\\p{M}+", "")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("-+", "-")
      .replaceAll("^-|-$", "");

    if (slug.isBlank()) {
      return "work-item";
    }

    if (slug.length() > MAX_SLUG_LENGTH) {
      return slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
    }

    return slug;
  }

  private String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
  }
}
