package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.autoforge.backend.domain.AuditEventType;
import org.autoforge.backend.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  @Mock
  private AuditLogRepository auditLogRepository;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private AuditService auditService;

  @Test
  void logsAuditEventAsJson() throws Exception {
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"foo\":\"bar\"}");
    when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var saved = auditService.logEvent("job-1", "system", AuditEventType.JOB_CREATED, Map.of("foo", "bar"));

    assertThat(saved.getDetailsJson()).isEqualTo("{\"foo\":\"bar\"}");
    verify(auditLogRepository).save(any());
  }

  @Test
  void fallsBackToEmptyJsonWhenSerializationFails() throws Exception {
    when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
    });
    when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var saved = auditService.logEvent("job-190", "system", AuditEventType.JOB_STARTED, Map.of("status", "RUNNING"));

    assertThat(saved.getDetailsJson()).isEqualTo("{}");
    assertThat(saved.getEventType()).isEqualTo(AuditEventType.JOB_STARTED);
    verify(auditLogRepository).save(any());
  }
}
