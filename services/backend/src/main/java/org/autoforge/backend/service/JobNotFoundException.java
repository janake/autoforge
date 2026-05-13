package org.autoforge.backend.service;

public class JobNotFoundException extends RuntimeException {

  public JobNotFoundException(String jobId) {
    super("Job not found: " + jobId);
  }
}
