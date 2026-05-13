package org.autoforge.backend.dto;

import java.util.List;

public record GeneratedPatchResponse(
  String patch,
  String summary,
  List<String> changedFiles
) {
}
