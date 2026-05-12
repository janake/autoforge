package org.autoforge.backend.domain;

public enum JobStatus {
  QUEUED,
  RUNNING,
  PATCH_GENERATED,
  PR_OPENED,
  FAILED
}
