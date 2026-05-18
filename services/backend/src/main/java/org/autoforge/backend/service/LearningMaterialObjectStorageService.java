package org.autoforge.backend.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LearningMaterialObjectStorageService {

  private static final String OBJECT_BUCKET_NAME = "learning-materials";

  public OriginalFileStoragePlan prepareOriginalFile(String ownerSubject, MultipartFile file) {
    if (ownerSubject == null || ownerSubject.isBlank()) {
      throw new IllegalArgumentException("Owner subject is required");
    }
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file must not be empty");
    }

    byte[] content = readContent(file);
    String contentHash = sha256Hex(content);
    String sanitizedOwner = sanitizePathSegment(ownerSubject);
    String sanitizedFilename = sanitizePathSegment(firstNonBlank(file.getOriginalFilename(), "uploaded-file"));
    String objectKey = "%s/%s-%s".formatted(sanitizedOwner, contentHash.substring(0, 16), sanitizedFilename);

    return new OriginalFileStoragePlan(
      objectKey,
      "oci://%s/%s".formatted(OBJECT_BUCKET_NAME, objectKey),
      contentHash,
      contentHash.substring(0, 32),
      file.getOriginalFilename(),
      file.getContentType(),
      file.getSize()
    );
  }

  private byte[] readContent(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read uploaded file", exception);
    }
  }

  private static String sha256Hex(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Missing SHA-256 algorithm", exception);
    }
  }

  private static String sanitizePathSegment(String value) {
    String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
      .replaceAll("\\p{M}+", "")
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("-+", "-")
      .replaceAll("^-|-$", "");

    return normalized.isBlank() ? "item" : normalized;
  }

  private static String firstNonBlank(String first, String fallback) {
    if (first != null && !first.isBlank()) {
      return first.trim();
    }
    if (fallback != null && !fallback.isBlank()) {
      return fallback.trim();
    }
    return "uploaded-file";
  }

  public record OriginalFileStoragePlan(
    String objectKey,
    String objectUri,
    String contentHash,
    String eTag,
    String originalFilename,
    String contentType,
    long sizeBytes
  ) {
  }
}
