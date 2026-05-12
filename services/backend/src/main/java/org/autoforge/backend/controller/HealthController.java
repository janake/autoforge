package org.autoforge.backend.controller;

import org.autoforge.backend.dto.HealthResponse;
import org.autoforge.backend.service.BackendInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

  private final BackendInfoService backendInfoService;

  public HealthController(BackendInfoService backendInfoService) {
    this.backendInfoService = backendInfoService;
  }

  @GetMapping("/health")
  public HealthResponse health() {
    return backendInfoService.health();
  }
}
