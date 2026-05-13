package org.autoforge.backend.git;

public class GitBrokerException extends RuntimeException {

  public GitBrokerException(String message) {
    super(message);
  }

  public GitBrokerException(String message, Throwable cause) {
    super(message, cause);
  }
}
