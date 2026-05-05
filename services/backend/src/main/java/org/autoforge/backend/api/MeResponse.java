package org.autoforge.backend.api;

import java.util.List;
import java.util.Map;

public record MeResponse(
  String subject,
  String username,
  String email,
  List<String> roles,
  Map<String, String> claims
) {
}
