package org.autoforge.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.dto.LearningContentGenerationResponse;
import org.autoforge.backend.dto.LearningContentSourceReference;
import org.autoforge.backend.repository.LearningGeneratedContentRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningContentGenerationService {

  private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\R\\s*\\R+");
  private static final int MAX_CHUNK_LENGTH = 450;
  private static final String FALLBACK_REASON = "Real AI is unavailable because the learning generation provider is not configured.";

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningGeneratedContentRepository learningGeneratedContentRepository;

  @Transactional
  public LearningContentGenerationResponse generateQuestions(String materialId, String subject) {
    LearningMaterial material = loadOwnedMaterial(materialId, subject);
    GeneratedContent generated = generateFromMaterial(material, LearningContentGenerationType.QUESTION_SET);
    LearningGeneratedContent saved = learningGeneratedContentRepository.save(LearningGeneratedContent.create(
      material.getId(),
      subject,
      LearningContentGenerationType.QUESTION_SET,
      generated.content(),
      generated.sourceText(),
      true,
      FALLBACK_REASON
    ));
    return toResponse(saved, generated.sources());
  }

  @Transactional
  public LearningContentGenerationResponse generateSummary(String materialId, String subject) {
    LearningMaterial material = loadOwnedMaterial(materialId, subject);
    GeneratedContent generated = generateFromMaterial(material, LearningContentGenerationType.SUMMARY);
    LearningGeneratedContent saved = learningGeneratedContentRepository.save(LearningGeneratedContent.create(
      material.getId(),
      subject,
      LearningContentGenerationType.SUMMARY,
      generated.content(),
      generated.sourceText(),
      true,
      FALLBACK_REASON
    ));
    return toResponse(saved, generated.sources());
  }

  @Transactional(readOnly = true)
  public List<LearningContentGenerationResponse> listGeneratedContent(String materialId, String subject) {
    loadOwnedMaterial(materialId, subject);
    return learningGeneratedContentRepository.findByMaterialIdAndOwnerSubjectOrderByCreatedAtDesc(materialId, subject).stream()
      .map(content -> toResponse(content, parseSources(content.getSourceReferences())))
      .toList();
  }

  private LearningMaterial loadOwnedMaterial(String materialId, String subject) {
    LearningMaterial material = learningMaterialRepository.findById(materialId)
      .orElseThrow(() -> new LearningMaterialNotFoundException(materialId));
    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      throw new LearningMaterialAccessDeniedException(materialId);
    }
    return material;
  }

  private GeneratedContent generateFromMaterial(LearningMaterial material, LearningContentGenerationType generationType) {
    List<LearningContentSourceReference> sources = buildSources(material);
    if (generationType == LearningContentGenerationType.SUMMARY) {
      return new GeneratedContent(summaryContent(material, sources), sourceText(sources), sources);
    }
    return new GeneratedContent(questionSetContent(material, sources), sourceText(sources), sources);
  }

  private String questionSetContent(LearningMaterial material, List<LearningContentSourceReference> sources) {
    List<String> lines = new ArrayList<>();
    lines.add("1. Mi a legfontosabb üzenete a tananyagnak?");
    if (!sources.isEmpty()) {
      lines.add("2. Hogyan kapcsolódik a(z) %s. chunk a fő témához?".formatted(sources.get(0).chunkIndex() + 1));
    }
    if (sources.size() > 1) {
      lines.add("3. Melyik részleteket kellene tovább gyakorolni a(z) %s. chunk alapján?".formatted(sources.get(1).chunkIndex() + 1));
    }
    lines.add("4. Milyen kulcsfogalmakat érdemes visszanézni a " + material.getTitle() + " tananyagból?");
    return String.join("\n", lines);
  }

  private String summaryContent(LearningMaterial material, List<LearningContentSourceReference> sources) {
    List<String> lines = new ArrayList<>();
    lines.add("- A tananyag címe: %s".formatted(material.getTitle()));
    if (material.getDescription() != null && !material.getDescription().isBlank()) {
      lines.add("- Leírás: %s".formatted(material.getDescription().trim()));
    }
    sources.stream().limit(3).forEach(source -> lines.add("- Chunk %d: %s".formatted(source.chunkIndex() + 1, source.excerpt())));
    return String.join("\n", lines);
  }

  private List<LearningContentSourceReference> buildSources(LearningMaterial material) {
    List<String> chunks = chunkMaterial(material);
    List<LearningContentSourceReference> sources = new ArrayList<>();
    for (int index = 0; index < chunks.size(); index++) {
      sources.add(new LearningContentSourceReference(index, excerpt(chunks.get(index))));
    }
    return sources;
  }

  private List<String> chunkMaterial(LearningMaterial material) {
    String text = materialText(material);
    List<String> chunks = new ArrayList<>();

    for (String paragraph : PARAGRAPH_SPLIT.split(text)) {
      if (paragraph == null || paragraph.isBlank()) {
        continue;
      }
      String remaining = paragraph.trim();
      while (remaining.length() > MAX_CHUNK_LENGTH) {
        chunks.add(remaining.substring(0, MAX_CHUNK_LENGTH));
        remaining = remaining.substring(MAX_CHUNK_LENGTH).trim();
      }
      if (!remaining.isBlank()) {
        chunks.add(remaining);
      }
    }

    if (chunks.isEmpty()) {
      chunks.add(text);
    }

    return chunks;
  }

  private static String materialText(LearningMaterial material) {
    if (material.getContent() == null || material.getContent().length == 0) {
      return fallbackText(material);
    }

    String text = new String(material.getContent(), StandardCharsets.UTF_8).trim();
    if (text.isBlank()) {
      return fallbackText(material);
    }
    return text;
  }

  private static String fallbackText(LearningMaterial material) {
    StringBuilder builder = new StringBuilder();
    if (material.getTitle() != null) {
      builder.append(material.getTitle().trim());
    }
    if (material.getDescription() != null && !material.getDescription().isBlank()) {
      if (!builder.isEmpty()) {
        builder.append("\n");
      }
      builder.append(material.getDescription().trim());
    }
    return builder.isEmpty() ? "Learning material" : builder.toString();
  }

  private static String excerpt(String content) {
    String normalized = content.replaceAll("\\s+", " ").trim();
    if (normalized.length() <= 120) {
      return normalized;
    }
    return normalized.substring(0, 117) + "...";
  }

  private static String sourceText(List<LearningContentSourceReference> sources) {
    return sources.stream()
      .map(source -> "chunk-%d: %s".formatted(source.chunkIndex(), source.excerpt()))
      .collect(Collectors.joining("\n"));
  }

  private LearningContentGenerationResponse toResponse(LearningGeneratedContent content, List<LearningContentSourceReference> sources) {
    return new LearningContentGenerationResponse(
      content.getId(),
      content.getMaterialId(),
      content.getGenerationType(),
      content.getContent(),
      sources,
      content.isFallbackUsed(),
      content.getFallbackReason(),
      content.getCreatedAt()
    );
  }

  private List<LearningContentSourceReference> parseSources(String sourceReferences) {
    if (sourceReferences == null || sourceReferences.isBlank()) {
      return List.of();
    }

    return sourceReferences.lines()
      .map(line -> line.replaceFirst("^chunk-", ""))
      .map(line -> line.split(":\\s*", 2))
      .filter(parts -> parts.length == 2)
      .map(parts -> new LearningContentSourceReference(parseChunkIndex(parts[0]), parts[1]))
      .toList();
  }

  private static int parseChunkIndex(String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (Exception exception) {
      return 0;
    }
  }

  private record GeneratedContent(String content, String sourceText, List<LearningContentSourceReference> sources) {
  }
}
