package org.autoforge.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.autoforge.backend.domain.AuditEventType;
import org.autoforge.backend.domain.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
  "spring.datasource.url=jdbc:h2:mem:auditdb;MODE=Oracle;DB_CLOSE_DELAY=-1",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.datasource.username=sa",
  "spring.datasource.password=",
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class AuditLogRepositoryTest {

  @Autowired
  private AuditLogRepository auditLogRepository;

  @Test
  void savesAndLoadsAuditLogWithGeneratedIdAndTimestamp() {
    AuditLog saved = auditLogRepository.saveAndFlush(
      AuditLog.create("job-1", "system", AuditEventType.JOB_CREATED, "{\"status\":\"QUEUED\"}")
    );

    assertThat(saved.getEventId()).isNotBlank();
    assertThat(saved.getTimestamp()).isNotNull();
    assertThat(auditLogRepository.findByJobIdOrderByTimestampAsc("job-1"))
      .hasSize(1);
    assertThat(auditLogRepository.findByJobIdOrderByTimestampAsc("job-1").get(0).getEventType())
      .isEqualTo(AuditEventType.JOB_CREATED);
  }
}
