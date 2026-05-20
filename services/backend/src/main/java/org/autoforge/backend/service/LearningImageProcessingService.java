package org.autoforge.backend.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;

@Service
public class LearningImageProcessingService {

  private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of(
    "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
  );

  private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

  private static final int MAX_DIMENSION_PX = 2048;
  private static final float JPEG_QUALITY = 0.85f;
  private static final long SMALL_FILE_THRESHOLD_BYTES = 200L * 1024;

  public record ImageProcessResult(
    byte[] originalContent,
    byte[] optimizedContent,
    String optimizedContentType,
    long optimizedFileSize,
    String contentHash,
    String optimizedContentHash,
    boolean optimized
  ) {
  }

  public boolean isImageMimeType(String mimeType) {
    return mimeType != null && SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType.toLowerCase());
  }

  public boolean isImageExtension(String extension) {
    return extension != null && SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
  }

  public ImageProcessResult process(byte[] content, String mimeType, String extension) {
    if (!isImageMimeType(mimeType) && !isImageExtension(extension)) {
      return new ImageProcessResult(content, null, null, 0, null, null, false);
    }

    String contentHash = sha256(content);

    BufferedImage original;
    try {
      original = ImageIO.read(new ByteArrayInputStream(content));
    } catch (IOException e) {
      return new ImageProcessResult(content, null, null, 0, contentHash, null, false);
    }

    if (original == null) {
      return new ImageProcessResult(content, null, null, 0, contentHash, null, false);
    }

    int width = original.getWidth();
    int height = original.getHeight();

    if (width <= MAX_DIMENSION_PX && height <= MAX_DIMENSION_PX && content.length <= SMALL_FILE_THRESHOLD_BYTES) {
      if ("image/jpeg".equalsIgnoreCase(mimeType)) {
        return new ImageProcessResult(content, content, "image/jpeg", content.length, contentHash, contentHash, false);
      }
      byte[] jpegBytes = convertToJpeg(original);
      String optimizedHash = sha256(jpegBytes);
      return new ImageProcessResult(content, jpegBytes, "image/jpeg", jpegBytes.length, contentHash, optimizedHash, true);
    }

    double scale = Math.min((double) MAX_DIMENSION_PX / width, (double) MAX_DIMENSION_PX / height);
    int newWidth = (int) (width * scale);
    int newHeight = (int) (height * scale);

    BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = resized.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
    g2d.dispose();

    byte[] optimizedBytes = convertToJpeg(resized);
    String optimizedHash = sha256(optimizedBytes);

    return new ImageProcessResult(
      content, optimizedBytes, "image/jpeg", optimizedBytes.length,
      contentHash, optimizedHash, true
    );
  }

  private byte[] convertToJpeg(BufferedImage image) {
    try {
      Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
      if (!writers.hasNext()) {
        throw new IllegalStateException("No JPEG writer available");
      }
      ImageWriter writer = writers.next();
      ImageWriteParam params = writer.getDefaultWriteParam();
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(JPEG_QUALITY);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
      }
      writer.dispose();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to convert image to JPEG", e);
    }
  }

  private static String sha256(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(data));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
