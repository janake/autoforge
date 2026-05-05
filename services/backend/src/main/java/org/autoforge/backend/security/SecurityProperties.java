package org.autoforge.backend.security;

import java.util.List;
import java.util.stream.Stream;

public record SecurityProperties(List<String> allowedOrigins) {

  static SecurityProperties fromCsv(String csv) {
    if (csv == null || csv.isBlank()) {
      return new SecurityProperties(List.of());
    }

    List<String> origins = Stream.of(csv.split(","))
      .map(String::trim)
      .filter(origin -> !origin.isBlank())
      .toList();
    return new SecurityProperties(origins);
  }
}
