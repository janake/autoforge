package org.autoforge.backend.security;

import java.util.List;

final class Lists {

  private Lists() {
  }

  static List<String> allowedMethods() {
    return List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  }

  static List<String> allowedHeaders() {
    return List.of("Authorization", "Cache-Control", "Content-Type", "X-Requested-With");
  }
}
