package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.autoforge.backend.domain.Job;
import org.junit.jupiter.api.Test;

class MockAIPatchGeneratorTest {

  private final MockAIPatchGenerator generator = new MockAIPatchGenerator();

  @Test
  void generatesDeterministicUnifiedDiff() {
    var response = generator.generatePatch(Job.createQueued("AUTO-194", "Generate a patch", "janake/autoforge", "main"));

    assertThat(response.summary()).isEqualTo("Mock patch for AUTO-194");
    assertThat(response.changedFiles()).containsExactly("autoforge-mock-ai.txt");
    assertThat(response.patch()).contains("diff --git a/autoforge-mock-ai.txt b/autoforge-mock-ai.txt");
    assertThat(response.patch()).contains("new file mode 100644");
    assertThat(response.patch()).contains("+++ b/autoforge-mock-ai.txt");
    assertThat(response.patch()).contains("+AUTO-194 sample patch");
  }
}
