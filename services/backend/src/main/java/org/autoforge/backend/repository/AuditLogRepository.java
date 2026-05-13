package org.autoforge.backend.repository;

import java.util.List;
import org.autoforge.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

  List<AuditLog> findByJobIdOrderByTimestampAsc(String jobId);
}
