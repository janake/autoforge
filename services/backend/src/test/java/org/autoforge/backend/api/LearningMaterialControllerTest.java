package org.autoforge.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.autoforge.backend.domain.LearningAssignmentTargetType;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialAssignment;
import org.autoforge.backend.repository.LearningMaterialAssignmentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

  @BeforeEach
  void cleanState() {
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

    mockMvc.perform(get("/api/v1/learning/materials/{materialId}", material.getId())
        .with(jwt().jwt(token -> token.subject("student-1").claim("groups", List.of("/group-b")))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.canManageAssignments").value(false))
      .andExpect(jsonPath("$.studentSubjects[0]").value("student-1"))
      .andExpect(jsonPath("$.groupNames[1]").value("group-b"));
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
}
