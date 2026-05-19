package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.autoforge.backend.domain.LearningMaterialSource;
import org.autoforge.backend.repository.LearningMaterialSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LearningMaterialSourceRepositoryTest {

  @Autowired
  private LearningMaterialSourceRepository learningMaterialSourceRepository;

  @BeforeEach
  void cleanState() {
    learningMaterialSourceRepository.deleteAll();
  }

  @Test
  void omitsSoftDeletedSourcesFromActiveQueries() {
    LearningMaterialSource active = learningMaterialSourceRepository.save(LearningMaterialSource.create(
      "material-1",
      "teacher-1",
      "PRIMARY_UPLOAD",
      "Algebra alapok",
      "algebra.pdf",
      "application/pdf",
      12L,
      "teacher-1/object-1",
      "oci://learning-materials/teacher-1/object-1",
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "0123456789abcdef0123456789abcdef",
      "content".getBytes(StandardCharsets.UTF_8)
    ));
    LearningMaterialSource deleted = learningMaterialSourceRepository.save(LearningMaterialSource.create(
      "material-1",
      "teacher-1",
      "ADDITIONAL_UPLOAD",
      "Jegyzet",
      "notes.txt",
      "text/plain",
      8L,
      "teacher-1/object-2",
      "oci://learning-materials/teacher-1/object-2",
      "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
      "abcdef0123456789abcdef0123456789",
      "content".getBytes(StandardCharsets.UTF_8)
    ));
    deleted.markDeleted();
    learningMaterialSourceRepository.save(deleted);

    assertThat(learningMaterialSourceRepository.findByMaterialIdAndDeletedAtIsNullOrderByCreatedAtAsc("material-1"))
      .extracting(LearningMaterialSource::getId)
      .containsExactly(active.getId());
  }
}
