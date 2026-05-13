package org.autoforge.backend.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

  @Id
  @Column(name = "event_id", nullable = false, updatable = false, length = 36)
  private String eventId;

  @Column(name = "job_id", nullable = false, length = 36)
  private String jobId;

  @Column(name = "user_subject", nullable = false, length = 128)
  private String userSubject;

  @Column(name = "event_timestamp", nullable = false)
  private Instant timestamp;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 32)
  private AuditEventType eventType;

  @Lob
  @Column(name = "details_json", nullable = false)
  private String detailsJson;

  private AuditLog(String jobId, String userSubject, AuditEventType eventType, String detailsJson) {
    this.jobId = jobId;
    this.userSubject = userSubject;
    this.eventType = eventType;
    this.detailsJson = detailsJson;
  }

  public static AuditLog create(String jobId, String userSubject, AuditEventType eventType, String detailsJson) {
    return new AuditLog(jobId, userSubject, eventType, detailsJson);
  }

  @PrePersist
  void prePersist() {
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }
}
