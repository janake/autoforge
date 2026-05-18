package org.autoforge.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.autoforge.backend.domain.LearningImageAsset;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.repository.LearningImageAssetRepository;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningImageAssetControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LearningMaterialRepository learningMaterialRepository;

  @Autowired
  private LearningMaterialAssignmentRepository learningMaterialAssignmentRepository;

  @Autowired
  private LearningImageAssetRepository learningImageAssetRepository;

  @BeforeEach
  void cleanState() {
    learningImageAssetRepository.deleteAll();
    learningMaterialAssignmentRepository.deleteAll();
    learningMaterialRepository.deleteAll();
  }

  @Test
  void ownerCanListImageAssetsAndProxyRedirectsToStorageUrl() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.create("teacher-1", "Fizika", "Diagramokkal"));
    LearningImageAsset asset = learningImageAssetRepository.save(LearningImageAsset.create(
      material.getId(),
      "teacher-1",
      "https://storage.example/assets/diagram-1.png",
      "image/png",
      2048L,
      "sha256:diagram-1",
      "Mágneses mező diagram"
    ));

    mockMvc.perform(get("/api/v1/learning/image-assets/material/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(asset.getId()))
      .andExpect(jsonPath("$[0].materialId").value(material.getId()))
      .andExpect(jsonPath("$[0].assetUrl").value("/api/v1/learning/image-assets/%s/proxy".formatted(asset.getId())))
      .andExpect(jsonPath("$[0].altText").value("Mágneses mező diagram"));

    mockMvc.perform(get("/api/v1/learning/image-assets/{assetId}/proxy", asset.getId())
        .with(jwt().jwt(token -> token.subject("teacher-1"))))
      .andExpect(status().isFound())
      .andExpect(header().string("Location", asset.getStorageObjectUri()));
  }

  @Test
  void unrelatedUserCannotListOrProxyImageAssets() throws Exception {
    LearningMaterial material = learningMaterialRepository.save(LearningMaterial.create("teacher-1", "Kémia", "Owner only"));
    LearningImageAsset asset = learningImageAssetRepository.save(LearningImageAsset.create(
      material.getId(),
      "teacher-1",
      "https://storage.example/assets/diagram-2.png",
      "image/png",
      1024L,
      "sha256:diagram-2",
      null
    ));

    mockMvc.perform(get("/api/v1/learning/image-assets/material/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2"))))
      .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/learning/image-assets/{assetId}/proxy", asset.getId())
        .with(jwt().jwt(token -> token.subject("teacher-2"))))
      .andExpect(status().isForbidden());
  }
}
