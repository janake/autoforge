package org.autoforge.backend.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FailingController {

  @GetMapping("/api/v1/fail")
  String fail() {
    throw new IllegalArgumentException("invalid request");
  }
}
