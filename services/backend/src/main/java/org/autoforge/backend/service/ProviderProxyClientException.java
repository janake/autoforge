package org.autoforge.backend.service;

public class ProviderProxyClientException extends RuntimeException {

  public ProviderProxyClientException(String message) {
    super(message);
  }

  public ProviderProxyClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
