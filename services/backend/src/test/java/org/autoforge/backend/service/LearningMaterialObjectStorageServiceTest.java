package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LearningMaterialObjectStorageServiceTest {

  private final LearningMaterialObjectStorageService service = new LearningMaterialObjectStorageService();

  @Test
  void preparesAStableStoragePlanForUploadedMaterials() {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "Algebra alapok.pdf",
      "application/pdf",
      "learning material body".getBytes(StandardCharsets.UTF_8)
    );

    LearningMaterialObjectStorageService.OriginalFileStoragePlan plan = service.prepareOriginalFile("Teacher 1", file);

    assertThat(plan.objectKey()).startsWith("teacher-1/");
    assertThat(plan.objectUri()).isEqualTo("oci://learning-materials/" + plan.objectKey());
    assertThat(plan.contentHash()).hasSize(64);
    assertThat(plan.eTag()).isEqualTo(plan.contentHash().substring(0, 32));
    assertThat(plan.originalFilename()).isEqualTo("Algebra alapok.pdf");
    assertThat(plan.contentType()).isEqualTo("application/pdf");
    assertThat(plan.sizeBytes()).isEqualTo(file.getSize());
  }

  @Test
  void rejectsEmptyUploadsAndMissingOwners() {
    MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

    assertThatThrownBy(() -> service.prepareOriginalFile("teacher-1", empty))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Uploaded file must not be empty");

    MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.prepareOriginalFile(" ", file))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Owner subject is required");
  }

  @Test
  void wrapsFileReadFailuresWhenPreparingStoragePlan() {
    MockMultipartFile unreadable = new MockMultipartFile(
      "file",
      "notes.txt",
      "text/plain",
      "content".getBytes(StandardCharsets.UTF_8)
    ) {
      @Override
      public byte[] getBytes() throws IOException {
        throw new IOException("object storage read failed");
      }
    };

    assertThatThrownBy(() -> service.prepareOriginalFile("teacher-1", unreadable))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to read uploaded file")
      .hasCauseInstanceOf(IOException.class);
  }
}
