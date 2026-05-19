package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "learning_question_disputes")
public class LearningQuestionDispute {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "attempt_id", nullable = false, length = 36)
  private String attemptId;

  @Column(name = "student_subject", nullable = false, length = 128)
  private String studentSubject;

  @Column(name = "question_index", nullable = false)
  private int questionIndex;

  @Column(name = "selected_option_index", nullable = false)
  private int selectedOptionIndex;

  @Column(name = "reason", nullable = false, length = 2000)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private LearningQuestionDisputeStatus status;

  @Column(name = "reviewer_subject", length = 128)
  private String reviewerSubject;

  @Column(name = "review_reason", length = 2000)
  private String reviewReason;

  @Column(name = "override_score")
  private Integer overrideScore;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  protected LearningQuestionDispute() {
  }

  private LearningQuestionDispute(String materialId, String attemptId, String studentSubject, int questionIndex, int selectedOptionIndex, String reason) {
    this.materialId = materialId;
    this.attemptId = attemptId;
    this.studentSubject = studentSubject;
    this.questionIndex = questionIndex;
    this.selectedOptionIndex = selectedOptionIndex;
    this.reason = reason;
    this.status = LearningQuestionDisputeStatus.OPEN;
  }

  public static LearningQuestionDispute create(String materialId, String attemptId, String studentSubject, int questionIndex, int selectedOptionIndex, String reason) {
    return new LearningQuestionDispute(materialId, attemptId, studentSubject, questionIndex, selectedOptionIndex, reason);
  }

  @PrePersist
  void prePersist() {
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
  }

  public String getId() { return id; }
  public String getMaterialId() { return materialId; }
  public String getAttemptId() { return attemptId; }
  public String getStudentSubject() { return studentSubject; }
  public int getQuestionIndex() { return questionIndex; }
  public int getSelectedOptionIndex() { return selectedOptionIndex; }
  public String getReason() { return reason; }
  public LearningQuestionDisputeStatus getStatus() { return status; }
  public String getReviewerSubject() { return reviewerSubject; }
  public String getReviewReason() { return reviewReason; }
  public Integer getOverrideScore() { return overrideScore; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getReviewedAt() { return reviewedAt; }

  public void review(String reviewerSubject, LearningQuestionDisputeStatus status, String reviewReason, Integer overrideScore) {
    this.reviewerSubject = reviewerSubject;
    this.status = status;
    this.reviewReason = reviewReason;
    this.overrideScore = overrideScore;
    this.reviewedAt = Instant.now();
  }
}
