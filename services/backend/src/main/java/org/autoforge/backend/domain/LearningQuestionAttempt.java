package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "learning_question_attempts")
public class LearningQuestionAttempt {

  @Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "material_id", nullable = false, length = 36)
  private String materialId;

  @Column(name = "generation_id", nullable = false, length = 36)
  private String generationId;

  @Column(name = "student_subject", nullable = false, length = 128)
  private String studentSubject;

  @Column(name = "score", nullable = false)
  private int score;

  @Column(name = "total_questions", nullable = false)
  private int totalQuestions;

  @Lob
  @Column(name = "answers", nullable = false)
  private String answers;

  @CreationTimestamp
  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  protected LearningQuestionAttempt() {
  }

  private LearningQuestionAttempt(String materialId, String generationId, String studentSubject, int score, int totalQuestions, String answers) {
    this.materialId = materialId;
    this.generationId = generationId;
    this.studentSubject = studentSubject;
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.answers = answers;
  }

  public static LearningQuestionAttempt create(String materialId, String generationId, String studentSubject, int score, int totalQuestions, String answers) {
    return new LearningQuestionAttempt(materialId, generationId, studentSubject, score, totalQuestions, answers);
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

  public int getScore() {
    return score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public String getAnswers() {
    return answers;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
