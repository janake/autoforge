package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "learning_learner_profiles")
public class LearningLearnerProfile {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "owner_subject", nullable = false, unique = true, length = 128)
  private String ownerSubject;

  @Column(name = "knowledge_level", nullable = false, length = 64)
  private String knowledgeLevel;

  @Column(name = "preferred_question_style", nullable = false, length = 128)
  private String preferredQuestionStyle;

  @Column(name = "preferred_explanation_style", nullable = false, length = 128)
  private String preferredExplanationStyle;

  @Column(name = "study_goal", length = 1000)
  private String studyGoal;

  @Column(name = "prompt_notes", length = 1000)
  private String promptNotes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LearningLearnerProfile() {
  }

  private LearningLearnerProfile(
    String ownerSubject,
    String knowledgeLevel,
    String preferredQuestionStyle,
    String preferredExplanationStyle,
    String studyGoal,
    String promptNotes
  ) {
    this.ownerSubject = ownerSubject;
    this.knowledgeLevel = knowledgeLevel;
    this.preferredQuestionStyle = preferredQuestionStyle;
    this.preferredExplanationStyle = preferredExplanationStyle;
    this.studyGoal = studyGoal;
    this.promptNotes = promptNotes;
  }

  public static LearningLearnerProfile create(String ownerSubject) {
    return new LearningLearnerProfile(ownerSubject, "beginner", "guided", "step-by-step", null, null);
  }

  public void update(
    String knowledgeLevel,
    String preferredQuestionStyle,
    String preferredExplanationStyle,
    String studyGoal,
    String promptNotes
  ) {
    if (knowledgeLevel != null) {
      this.knowledgeLevel = knowledgeLevel;
    }
    if (preferredQuestionStyle != null) {
      this.preferredQuestionStyle = preferredQuestionStyle;
    }
    if (preferredExplanationStyle != null) {
      this.preferredExplanationStyle = preferredExplanationStyle;
    }
    if (studyGoal != null) {
      this.studyGoal = studyGoal;
    }
    if (promptNotes != null) {
      this.promptNotes = promptNotes;
    }
  }

  @PrePersist
  void prePersist() {
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
  }

  public String getId() {
    return id;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public String getKnowledgeLevel() {
    return knowledgeLevel;
  }

  public String getPreferredQuestionStyle() {
    return preferredQuestionStyle;
  }

  public String getPreferredExplanationStyle() {
    return preferredExplanationStyle;
  }

  public String getStudyGoal() {
    return studyGoal;
  }

  public String getPromptNotes() {
    return promptNotes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
