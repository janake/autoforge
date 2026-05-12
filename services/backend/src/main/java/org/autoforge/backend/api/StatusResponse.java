package org.autoforge.backend.api;

public record StatusResponse(String service, String status, String stack) {
}
