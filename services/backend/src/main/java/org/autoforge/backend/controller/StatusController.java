package org.autoforge.backend.controller;

import org.autoforge.backend.dto.StatusResponse;
import org.autoforge.backend.service.BackendInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class StatusController {

  private final BackendInfoService backendInfoService;

  public StatusController(BackendInfoService backendInfoService) {
    this.backendInfoService = backendInfoService;
  }

  @GetMapping("/status")
  public StatusResponse status() {
    return backendInfoService.status();
  }
}
