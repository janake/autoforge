package org.autoforge.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.domain.LearningGenerationStatus;
import org.autoforge.backend.domain.LearningGeneratedContent;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.dto.LearningContentGenerationResponse;
import org.autoforge.backend.dto.LearningContentSourceReference;
import org.autoforge.backend.dto.LearningQuestionOptionPayload;
import org.autoforge.backend.dto.LearningQuestionPayload;
import org.autoforge.backend.dto.LearningQuestionSetPayload;
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
  private final LearningLearnerProfileService learningLearnerProfileService;
  private final ObjectMapper objectMapper;

  @Transactional
  public LearningContentGenerationResponse generateQuestions(String materialId, String subject) {
    LearningMaterial material = loadOwnedMaterial(materialId, subject);
    GeneratedContent generated = generateFromMaterial(material, subject, LearningContentGenerationType.QUESTION_SET);
    LearningGeneratedContent saved = learningGeneratedContentRepository.save(LearningGeneratedContent.create(
      material.getId(),
      subject,
      LearningContentGenerationType.QUESTION_SET,
      generated.content(),
      generated.structuredContent(),
      generated.sourceText(),
      true,
      FALLBACK_REASON,
      LearningGenerationStatus.COMPLETED,
      null
    ));
    return toResponse(saved, generated.sources());
  }

  @Transactional
  public LearningContentGenerationResponse generateSummary(String materialId, String subject) {
    LearningMaterial material = loadOwnedMaterial(materialId, subject);
    GeneratedContent generated = generateFromMaterial(material, subject, LearningContentGenerationType.SUMMARY);
    LearningGeneratedContent saved = learningGeneratedContentRepository.save(LearningGeneratedContent.create(
      material.getId(),
      subject,
      LearningContentGenerationType.SUMMARY,
      generated.content(),
      null,
      generated.sourceText(),
      true,
      FALLBACK_REASON,
      LearningGenerationStatus.COMPLETED,
      null
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

  private GeneratedContent generateFromMaterial(LearningMaterial material, String subject, LearningContentGenerationType generationType) {
    List<LearningContentSourceReference> sources = buildSources(material);
    String retrievalContext = learningLearnerProfileService.buildRetrievalContext(subject);
    if (generationType == LearningContentGenerationType.SUMMARY) {
      return new GeneratedContent(summaryContent(material, sources, retrievalContext), null, sourceText(sources), sources);
    }
    LearningQuestionSetPayload payload = questionSetPayload(material, sources, retrievalContext);
    return new GeneratedContent(questionSetContent(payload), writeJson(payload), sourceText(sources), sources);
  }

  private LearningQuestionSetPayload questionSetPayload(LearningMaterial material, List<LearningContentSourceReference> sources, String retrievalContext) {
    List<LearningQuestionPayload> questions = new ArrayList<>();
    questions.add(new LearningQuestionPayload(
      "Mi a legfontosabb üzenete a tananyagnak?",
      standardOptions("A tananyag fő üzenete", "Egy mellékes részlet", "Egy nem kapcsolódó példa", "Egy későbbi fejezet"),
      0,
      "A fő üzenet a tananyag címéhez és a tanulói profilhoz igazodik.",
      sources.isEmpty() ? List.of() : List.of(sources.get(0)),
      null
    ));

    if (!sources.isEmpty()) {
      questions.add(new LearningQuestionPayload(
        "Hogyan kapcsolódik a(z) %s. chunk a fő témához?".formatted(sources.get(0).chunkIndex() + 1),
        standardOptions("A fő témát támogatja", "Eltér a témától", "Csak adminisztratív metaadat", "Teljesen üres"),
        0,
        "Az első chunk a forrás összefoglalóját és a tananyag fő gondolatát hordozza.",
        List.of(sources.get(0)),
        null
      ));
    }

    if (sources.size() > 1) {
      questions.add(new LearningQuestionPayload(
        "Melyik részletet kellene tovább gyakorolni a(z) %s. chunk alapján?".formatted(sources.get(1).chunkIndex() + 1),
        standardOptions("A kulcsfogalmak gyakorlását", "Csak a fájlnevet", "A tananyag törlését", "A hitelesítő adatokat"),
        0,
        "A második chunk kifejezetten a gyakorlásra érdemes kulcsfogalmakat emeli ki.",
        List.of(sources.get(1)),
        null
      ));
    }

    questions.add(new LearningQuestionPayload(
      "Milyen kulcsfogalmakat érdemes visszanézni a %s tananyagból?".formatted(material.getTitle()),
      standardOptions("A definíciókat és összefüggéseket", "A képernyő színét", "A feltöltés időpontját", "A JWT kódolását"),
      0,
      "A visszanézendő fogalmak a tartalom megértését támogatják, nem a technikai metaadatokat.",
      sources.stream().limit(2).toList(),
      null
    ));

    return new LearningQuestionSetPayload(material.getId(), material.getTitle(), retrievalContext, questions);
  }

  private String questionSetContent(LearningQuestionSetPayload payload) {
    List<String> lines = new ArrayList<>();
    lines.add("Tanulói profil:");
    lines.add(payload.retrievalContext());
    lines.add("");
    int index = 1;
    for (LearningQuestionPayload question : payload.questions()) {
      lines.add(index++ + ". " + question.prompt());
      for (LearningQuestionOptionPayload option : question.options()) {
        lines.add("   " + option.key() + ") " + option.text());
      }
      lines.add("   Helyes válasz: " + question.options().get(question.correctOptionIndex()).key());
      lines.add("   Magyarázat: " + question.explanation());
      if (!question.sources().isEmpty()) {
        lines.add("   Forrás chunkok: " + question.sources().stream().map(source -> Integer.toString(source.chunkIndex() + 1)).collect(Collectors.joining(", ")));
      }
      if (question.imageAssetReference() != null && !question.imageAssetReference().isBlank()) {
        lines.add("   Kép: " + question.imageAssetReference());
      }
      lines.add("");
    }
    return String.join("\n", lines);
  }

  private String summaryContent(LearningMaterial material, List<LearningContentSourceReference> sources, String retrievalContext) {
    List<String> lines = new ArrayList<>();
    lines.add("Tanulói profil:");
    lines.add(retrievalContext);
    lines.add("");
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

  private List<LearningQuestionOptionPayload> standardOptions(String correctText, String distractorOne, String distractorTwo, String distractorThree) {
    return List.of(
      new LearningQuestionOptionPayload("A", correctText),
      new LearningQuestionOptionPayload("B", distractorOne),
      new LearningQuestionOptionPayload("C", distractorTwo),
      new LearningQuestionOptionPayload("D", distractorThree)
    );
  }

  private String writeJson(LearningQuestionSetPayload payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize learning question set", exception);
    }
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
      content.getGenerationStatus(),
      content.getStructuredContent(),
      content.getErrorMessage(),
      content.getCreatedAt()
    );
  }

  public LearningContentGenerationResponse toResponse(LearningGeneratedContent content) {
    return toResponse(content, parseSources(content.getSourceReferences()));
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

  private record GeneratedContent(String content, String structuredContent, String sourceText, List<LearningContentSourceReference> sources) {
  }
}
