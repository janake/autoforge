package org.autoforge.backend.dto;

public record HealthResponse(String service, String status, String stack) {
}
