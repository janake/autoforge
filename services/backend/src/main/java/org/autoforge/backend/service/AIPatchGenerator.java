package org.autoforge.backend.service;

import org.autoforge.backend.domain.Job;
import org.autoforge.backend.dto.GeneratedPatchResponse;

public interface AIPatchGenerator {

  GeneratedPatchResponse generatePatch(Job job);
}
