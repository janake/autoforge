package org.autoforge.backend.service;

import org.autoforge.backend.dto.HealthResponse;
import org.autoforge.backend.dto.StatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BackendInfoService {

  private final String applicationName;

  public BackendInfoService(@Value("${spring.application.name}") String applicationName) {
    this.applicationName = applicationName;
  }

  public StatusResponse status() {
    return new StatusResponse(applicationName, "ok", "spring");
  }

  public HealthResponse health() {
    return new HealthResponse(applicationName, "UP", "spring");
  }
}
