package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "learning_question_progress")
public class LearningQuestionProgress {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "generation_id", nullable = false, length = 36)
  private String generationId;

  @Column(name = "student_subject", nullable = false, length = 128)
  private String studentSubject;

  @Column(name = "student_groups", length = 1000)
  private String studentGroups;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private LearningQuestionProgressStatus status;

  @Column(name = "attempt_id", length = 36)
  private String attemptId;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "score")
  private Integer score;

  @Column(name = "total_questions")
  private Integer totalQuestions;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected LearningQuestionProgress() {
  }

  private LearningQuestionProgress(String materialId, String generationId, String studentSubject) {
    this.materialId = materialId;
    this.generationId = generationId;
    this.studentSubject = studentSubject;
    this.status = LearningQuestionProgressStatus.ASSIGNED;
  }

  public static LearningQuestionProgress create(String materialId, String generationId, String studentSubject) {
    return new LearningQuestionProgress(materialId, generationId, studentSubject);
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

  public String getMaterialId() {
    return materialId;
  }

  public String getGenerationId() {
    return generationId;
  }

  public String getStudentSubject() {
    return studentSubject;
  }

  public String getStudentGroups() {
    return studentGroups;
  }

  public LearningQuestionProgressStatus getStatus() {
    return status;
  }

  public String getAttemptId() {
    return attemptId;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public Integer getScore() {
    return score;
  }

  public Integer getTotalQuestions() {
    return totalQuestions;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void start() {
    if (startedAt == null) {
      startedAt = Instant.now();
    }
    if (status == LearningQuestionProgressStatus.ASSIGNED) {
      status = LearningQuestionProgressStatus.STARTED;
    }
  }

  public void submit(String attemptId, int score, int totalQuestions) {
    this.attemptId = attemptId;
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.attemptCount++;
    if (startedAt == null) {
      startedAt = Instant.now();
    }
    this.submittedAt = Instant.now();
    this.status = LearningQuestionProgressStatus.SUBMITTED;
  }

  public void review(boolean completed, Integer overrideScore, Integer score, Integer totalQuestions) {
    if (overrideScore != null) {
      this.score = overrideScore;
    } else if (score != null) {
      this.score = score;
    }
    this.totalQuestions = totalQuestions;
    this.reviewedAt = Instant.now();
    if (completed) {
      this.completedAt = this.reviewedAt;
      this.status = LearningQuestionProgressStatus.COMPLETED;
    } else {
      this.status = LearningQuestionProgressStatus.REVIEWED;
    }
  }

  public void setStudentGroups(Collection<String> groups) {
    if (groups == null) {
      return;
    }

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String group : groups) {
      if (group == null) {
        continue;
      }
      String value = group.trim();
      if (value.isBlank()) {
        continue;
      }
      if (value.startsWith("/")) {
        value = value.substring(1);
      }
      if (value.contains("/")) {
        value = value.substring(value.lastIndexOf('/') + 1);
      }
      if (!value.isBlank()) {
        normalized.add(value);
      }
    }

    if (!normalized.isEmpty()) {
      this.studentGroups = String.join(",", normalized);
    }
  }

  public boolean hasStudentGroup(String group) {
    if (studentGroups == null || group == null) {
      return false;
    }
    for (String value : studentGroups.split(",")) {
      if (Objects.equals(value, group)) {
        return true;
      }
    }
    return false;
  }
}
