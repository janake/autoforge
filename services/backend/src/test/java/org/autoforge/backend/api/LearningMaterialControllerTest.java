package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.domain.LearningMaterialSource;
import org.autoforge.backend.repository.LearningAssignmentAuditRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningMaterialSourceRepository;
import org.autoforge.backend.repository.LearningQuestionProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningMaterialControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningMaterialRepository learningMaterialRepository;

  @Autowired
  private LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;

  @Autowired
  private LearningAssignmentAuditRepository learningAssignmentAuditRepository;

  @Autowired
  private LearningMaterialSourceRepository learningMaterialSourceRepository;

  @Autowired
  private LearningQuestionProgressRepository learningQuestionProgressRepository;

  @BeforeEach
  void cleanState() {
    learningAssignmentAuditRepository.deleteAll();
    learningQuestionProgressRepository.deleteAll();
    learningMaterialSourceRepository.deleteAll();
    learningMaterialAssignmentRepository.deleteAll();
    learningMaterialRepository.deleteAll();
  }

  @Test
  void ownerCanAssignStudentsAndGroupsAndSeeUpdatedDetail() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.create("teacher-1", "Algebra alapok", "Bevezető tananyag"));

    mockMvc.perform(put("/api/v1/learning/materials/{materialId}/assignments", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "studentSubjects": ["student-1", "student-2"],
            "groupNames": ["/group-a", "group-b"]
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(material.getId()))
      .andExpect(jsonPath("$.ownerSubject").value("teacher-1"))
      .andExpect(jsonPath("$.canManageAssignments").value(true))
      .andExpect(jsonPath("$.studentSubjects[0]").value("student-1"))
      .andExpect(jsonPath("$.studentSubjects[1]").value("student-2"))
      .andExpect(jsonPath("$.groupNames[0]").value("group-a"))
      .andExpect(jsonPath("$.groupNames[1]").value("group-b"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/assignment-audit", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(5))
      .andExpect(jsonPath("$[*].action", containsInAnyOrder("UPDATE", "CREATE", "CREATE", "CREATE", "CREATE")))
      .andExpect(jsonPath("$[*].targetType", containsInAnyOrder("MATERIAL", "STUDENT", "STUDENT", "GROUP", "GROUP")));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1").claim("groups", List.of("/group-b")))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.canManageAssignments").value(false))
      .andExpect(jsonPath("$.studentSubjects[0]").value("student-1"))
      .andExpect(jsonPath("$.groupNames[1]").value("group-b"));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/assignment-audit", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1").claim("groups", List.of("/group-b")))))
      .andExpect(status().isForbidden());
  }

  @Test
  void listReturnsOnlyAccessibleMaterials() throws Exception {
    LearningMaterial owned = learningMaterialRepository.save(LearningMaterial.create("teacher-1", "Saját tananyag", "owner only"));
    LearningMaterial assigned = learningMaterialRepository.save(LearningMaterial.create("teacher-2", "Csoportos tananyag", "group access"));
    learningMaterialAssignmentRepository.save(LearningMaterialAssignment.create(assigned.getId(), LearningAssignmentTargetType.GROUP, "group-a"));

    mockMvc.perform(get("/api/v1/learning/materials")
        .with(jwt().jwt(token -> token.subject("student-1").claim("groups", List.of("/group-a")))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(assigned.getId()))
      .andExpect(jsonPath("$[1].id").doesNotExist());

    mockMvc.perform(get("/api/v1/learning/materials")
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(owned.getId()))
      .andExpect(jsonPath("$[1].id").doesNotExist());
  }

  @Test
  void nonOwnerCannotReplaceAssignments() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.create("teacher-1", "Fizika", "owner only"));

    mockMvc.perform(put("/api/v1/learning/materials/{materialId}/assignments", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2")))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "studentSubjects": ["student-1"],
            "groupNames": ["group-a"]
          }
          """))
      .andExpect(status().isForbidden());

    assertThat(learningMaterialAssignmentRepository.findByMaterialId(material.getId())).isEmpty();
  }

  @Test
  void ownerCanUploadLearningMaterialWithMetadata() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "algebra.pdf",
      "application/pdf",
      "%PDF-1.4 learning material".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .param("title", "Algebra alapok")
        .param("description", "Bevezető feltöltött tananyag")
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.title").value("Algebra alapok"))
      .andExpect(jsonPath("$.originalFilename").value("algebra.pdf"))
      .andExpect(jsonPath("$.contentType").value("application/pdf"))
      .andExpect(jsonPath("$.fileSize").value(file.getSize()))
      .andExpect(jsonPath("$.storageObjectKey").value(org.hamcrest.Matchers.startsWith("teacher-1/")))
      .andExpect(jsonPath("$.storageObjectUri").value(org.hamcrest.Matchers.startsWith("oci://learning-materials/")))
      .andExpect(jsonPath("$.contentHash").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{64}")))
      .andExpect(jsonPath("$.contentETag").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}")))
      .andExpect(jsonPath("$.ownerSubject").value("teacher-1"))
      .andExpect(jsonPath("$.canManageAssignments").value(true))
      .andExpect(jsonPath("$.sources[0].sourceType").value("PRIMARY_UPLOAD"))
      .andExpect(jsonPath("$.sources[0].sourceName").value("Algebra alapok"));

    LearningMaterial stored = learningMaterialRepository.findAll().get(0);
    assertThat(stored.getOwnerSubject()).isEqualTo("teacher-1");
    assertThat(stored.getOriginalFilename()).isEqualTo("algebra.pdf");
    assertThat(stored.getContentType()).isEqualTo("application/pdf");
    assertThat(stored.getFileSize()).isEqualTo(file.getSize());
    assertThat(stored.getStorageObjectKey()).startsWith("teacher-1/");
    assertThat(stored.getStorageObjectUri()).startsWith("oci://learning-materials/");
    assertThat(stored.getContentHash()).matches("[0-9a-f]{64}");
    assertThat(stored.getContentETag()).matches("[0-9a-f]{32}");
    assertThat(stored.getContent()).isEqualTo(file.getBytes());
    assertThat(learningMaterialSourceRepository.findByMaterialIdAndDeletedAtIsNullOrderByCreatedAtAsc(stored.getId())).hasSize(1);
  }

  @Test
  void studentCannotUploadLearningMaterial() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "notes.txt",
      "text/plain",
      "student content".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .with(jwt().jwt(token -> token.subject("student-1").claim("realm_access", Map.of("roles", List.of("student"))))))
      .andExpect(status().isForbidden());

    assertThat(learningMaterialRepository.findAll()).isEmpty();
    assertThat(learningMaterialSourceRepository.findAll()).isEmpty();
  }

  @Test
  void ownerCanAddAndDeleteMaterialSources() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.create("teacher-1", "Több forrás", "source lifecycle"));

    MockMultipartFile sourceFile = new MockMultipartFile(
      "file",
      "notes.txt",
      "text/plain",
      "Első forrás szövege".getBytes(StandardCharsets.UTF_8)
    );

    String addResponse = mockMvc.perform(multipart("/api/v1/learning/materials/{materialId}/sources", material.getId())
        .file(sourceFile)
        .param("sourceName", "Jegyzet 1")
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.sources[0].sourceName").value("Jegyzet 1"))
      .andExpect(jsonPath("$.sources[0].sourceType").value("ADDITIONAL_UPLOAD"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String sourceId = com.jayway.jsonpath.JsonPath.read(addResponse, "$.sources[0].id");

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/learning/materials/{materialId}/sources/{sourceId}", material.getId(), sourceId)
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.sources").isEmpty());

    LearningMaterialSource deletedSource = learningMaterialSourceRepository.findById(sourceId).orElseThrow();
    assertThat(deletedSource.getDeletedAt()).isNotNull();
  }

  @Test
  void uploadRejectsUnsupportedFileFormat() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "notes.exe",
      "application/octet-stream",
      "binary".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isBadRequest());

    assertThat(learningMaterialRepository.findAll()).isEmpty();
  }

  @Test
  void uploadRejectsFilesOverTheLimit() throws Exception {
    byte[] content = new byte[10 * 1024 * 1024 + 1];
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "big.pdf",
      "application/pdf",
      content
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isBadRequest());

    assertThat(learningMaterialRepository.findAll()).isEmpty();
  }

  @Test
  void otherUserCannotReadUploadedMaterial() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.createUploaded(
      "teacher-1",
      "Algebra alapok",
      "Bevezető tananyag",
      "algebra.pdf",
      "application/pdf",
      27L,
      "%PDF-1.4 learning material".getBytes(StandardCharsets.UTF_8)
    ));

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2"))))
      .andExpect(status().isForbidden());
  }

  @Test
  void teacherCanUploadImageMaterial() throws Exception {
    byte[] imageBytes = createTestImage(4000, 3000);
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "diagram.png",
      "image/png",
      imageBytes
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .param("title", "Teszt diagram")
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.title").value("Teszt diagram"))
      .andExpect(jsonPath("$.contentType").value("image/png"))
      .andExpect(jsonPath("$.optimizedImageUrl").value(org.hamcrest.Matchers.endsWith("/optimized-image")))
      .andExpect(jsonPath("$.optimizedFileSize").isNumber())
      .andExpect(jsonPath("$.optimizedContentHash").isString());

    LearningMaterial stored = learningMaterialRepository.findAll().get(0);
    assertThat(stored.getOptimizedContent()).isNotNull();
    assertThat(stored.getOptimizedContentType()).isEqualTo("image/jpeg");
    assertThat(stored.getOptimizedFileSize()).isPositive();
    assertThat(stored.getContent()).isEqualTo(imageBytes);
    assertThat(stored.getContentHash()).matches("[0-9a-f]{64}");
    assertThat(stored.getOptimizedContentHash()).matches("[0-9a-f]{64}");
  }

  @Test
  void teacherCanUploadSmallImageWithoutResize() throws Exception {
    byte[] imageBytes = createTestImage(100, 80);
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "icon.png",
      "image/png",
      imageBytes
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.optimizedImageUrl").exists());
  }

  @Test
  void uploadRejectsUnsupportedImageFormat() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "notes.exe",
      "application/octet-stream",
      "binary".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isBadRequest());

    assertThat(learningMaterialRepository.findAll()).isEmpty();
  }

  @Test
  void optimizedImageIsAccessibleAfterImageUpload() throws Exception {
    byte[] imageBytes = createTestImage(200, 200);
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "chart.png",
      "image/png",
      imageBytes
    );

    String createResponse = mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .param("title", "Chart")
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();

    String materialId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

    byte[] fetchedBytes = mockMvc.perform(get("/api/v1/learning/materials/{materialId}/optimized-image", materialId)
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(content().contentType("image/jpeg"))
      .andReturn()
      .getResponse()
      .getContentAsByteArray();
    assertThat(fetchedBytes).isNotEmpty();
  }

  @Test
  void optimizedImageReturns404ForTextUpload() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "notes.pdf",
      "application/pdf",
      "%PDF-1.4 content".getBytes(StandardCharsets.UTF_8)
    );

    String createResponse = mockMvc.perform(multipart("/api/v1/learning/materials")
        .file(file)
        .with(jwt().jwt(token -> token.subject("teacher-1").claim("realm_access", Map.of("roles", List.of("teacher"))))))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();

    String materialId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}/optimized-image", materialId)
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isNotFound());
  }

  private static byte[] createTestImage(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D g2d = image.createGraphics();
    g2d.setColor(java.awt.Color.WHITE);
    g2d.fillRect(0, 0, width, height);
    g2d.setColor(java.awt.Color.BLACK);
    g2d.drawString("Test Image", 10, 30);
    g2d.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);
    return baos.toByteArray();
  }
}
