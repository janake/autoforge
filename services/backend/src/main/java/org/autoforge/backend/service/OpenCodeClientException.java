package org.autoforge.backend.service;

public class OpenCodeClientException extends RuntimeException {

  public OpenCodeClientException(String message) {
    super(message);
  }

  public OpenCodeClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
