package org.autoforge.backend.config;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import org.autoforge.backend.dto.ApiErrorResponse;
import org.autoforge.backend.service.JobNotFoundException;
import org.autoforge.backend.service.LearningMaterialAccessDeniedException;
import org.autoforge.backend.service.LearningMaterialNotFoundException;
import org.autoforge.backend.service.LearningIngestionAccessDeniedException;
import org.autoforge.backend.service.LearningIngestionNotFoundException;
import org.autoforge.backend.service.PromptDraftApprovalException;
import org.autoforge.backend.service.PromptDraftNotFoundException;
import org.autoforge.backend.service.PromptDraftTicketException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Request validation failed", request.getRequestURI());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Uploaded file exceeds the maximum allowed size", request.getRequestURI());
  }

  @ExceptionHandler(JobNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(JobNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(PromptDraftNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleDraftNotFound(PromptDraftNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(PromptDraftApprovalException.class)
  public ResponseEntity<ApiErrorResponse> handleDraftApproval(PromptDraftApprovalException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(PromptDraftTicketException.class)
  public ResponseEntity<ApiErrorResponse> handleDraftTicket(PromptDraftTicketException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(LearningMaterialNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleLearningMaterialNotFound(LearningMaterialNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(LearningMaterialAccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleLearningMaterialAccessDenied(LearningMaterialAccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(LearningIngestionNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleLearningIngestionNotFound(LearningIngestionNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(LearningIngestionAccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleLearningIngestionAccessDenied(LearningIngestionAccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
    String resolvedMessage = message == null || message.isBlank() ? status.getReasonPhrase() : message;
    return ResponseEntity.status(status).body(new ApiErrorResponse(
      Instant.now(),
      status.value(),
      status.getReasonPhrase(),
      resolvedMessage,
      path
    ));
  }
}
