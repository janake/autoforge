package org.autoforge.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.autoforge.backend.domain.LearningChunk;
import org.autoforge.backend.domain.LearningEmbedding;
import org.autoforge.backend.domain.LearningIngestionJob;
import org.autoforge.backend.domain.LearningIngestionStatus;
import org.autoforge.backend.domain.LearningMaterial;
import org.autoforge.backend.domain.LearningMaterialSource;
import org.autoforge.backend.dto.LearningEmbeddingPayload;
import org.autoforge.backend.dto.LearningIngestionResponse;
import org.autoforge.backend.repository.LearningChunkRepository;
import org.autoforge.backend.repository.LearningEmbeddingRepository;
import org.autoforge.backend.repository.LearningIngestionJobRepository;
import org.autoforge.backend.repository.LearningMaterialRepository;
import org.autoforge.backend.repository.LearningMaterialSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningIngestionService {

  private static final int CHUNK_SIZE = 800;
  private static final Pattern CHUNK_BOUNDARY = Pattern.compile("\\R\\s*\\R+");

  private final LearningMaterialRepository learningMaterialRepository;
  private final LearningIngestionJobRepository learningIngestionJobRepository;
  private final LearningChunkRepository learningChunkRepository;
  private final LearningEmbeddingRepository learningEmbeddingRepository;
  private final LearningMaterialSourceRepository learningMaterialSourceRepository;
  private final LearningEmbeddingProvider learningEmbeddingProvider;

  @Transactional
  public LearningIngestionResponse startIngestion(String materialId, String subject) {
    LearningMaterial material = loadOwnedMaterial(materialId, subject);
    LearningIngestionJob job = learningIngestionJobRepository.findByMaterialId(materialId)
      .map(existing -> {
        if (existing.getStatus() == LearningIngestionStatus.PROCESSING) {
          return existing;
        }
        List<String> chunkIds = existingChunkIds(existing.getId());
        if (!chunkIds.isEmpty()) {
          learningEmbeddingRepository.deleteByChunkIdIn(chunkIds);
        }
        learningChunkRepository.deleteByJobId(existing.getId());
        existing.markProcessing();
        return existing;
      })
      .orElseGet(() -> learningIngestionJobRepository.save(LearningIngestionJob.createQueued(materialId, subject)));

    if (job.getStatus() != LearningIngestionStatus.PROCESSING) {
      job.markProcessing();
    }

    return processJob(job, material);
  }

  @Transactional(readOnly = true)
  public LearningIngestionResponse getIngestion(String materialId, String subject) {
    LearningIngestionJob job = loadOwnedJob(materialId, subject);
    return toResponse(job);
  }

  @Transactional
  public LearningIngestionResponse retryIngestion(String materialId, String subject) {
    LearningMaterial material = loadOwnedMaterial(materialId, subject);
    LearningIngestionJob job = learningIngestionJobRepository.findByMaterialId(materialId)
      .orElseThrow(() -> new LearningIngestionNotFoundException(materialId));

    if (!Objects.equals(job.getOwnerSubject(), subject)) {
      throw new LearningIngestionAccessDeniedException(materialId);
    }

    job.incrementRetryCount();
    job.markProcessing();
    List<String> chunkIds = existingChunkIds(job.getId());
    if (!chunkIds.isEmpty()) {
      learningEmbeddingRepository.deleteByChunkIdIn(chunkIds);
    }
    learningChunkRepository.deleteByJobId(job.getId());

    return processJob(job, material);
  }

  @Transactional(readOnly = true)
  public LearningIngestionResponse summarizeMaterial(String materialId, String subject) {
    LearningIngestionJob job = learningIngestionJobRepository.findByMaterialId(materialId)
      .orElseThrow(() -> new LearningIngestionNotFoundException(materialId));
    if (!Objects.equals(job.getOwnerSubject(), subject)) {
      throw new LearningIngestionAccessDeniedException(materialId);
    }
    return toResponse(job);
  }

  private LearningIngestionResponse processJob(LearningIngestionJob job, LearningMaterial material) {
    try {
      String text = extractText(material);
      List<ChunkSlice> chunks = chunkText(text);

      if (chunks.isEmpty()) {
        throw new IllegalArgumentException("No chunkable content found");
      }

      List<LearningChunk> persistedChunks = new ArrayList<>();
      for (int index = 0; index < chunks.size(); index++) {
        ChunkSlice chunkSlice = chunks.get(index);
        LearningChunk chunk = LearningChunk.create(
          job.getId(),
          material.getId(),
          job.getOwnerSubject(),
          index,
          chunkSlice.content(),
          chunkSlice.sourceStartOffset(),
          chunkSlice.sourceEndOffset(),
          estimateTokens(chunkSlice.content())
        );
        persistedChunks.add(learningChunkRepository.save(chunk));

        LearningEmbeddingPayload payload = learningEmbeddingProvider.embed(chunkSlice.content());
        learningEmbeddingRepository.save(LearningEmbedding.create(chunk.getId(), job.getOwnerSubject(), payload.model(), payload.dimensions(), payload.vectorJson()));
      }

      job.markCompleted();
      return toResponse(job, persistedChunks.size());
    } catch (Exception exception) {
      job.markFailed(failureMessage(exception));
      return toResponse(job, 0);
    }
  }

  private LearningMaterial loadOwnedMaterial(String materialId, String subject) {
    LearningMaterial material = learningMaterialRepository.findById(materialId)
      .orElseThrow(() -> new LearningIngestionNotFoundException(materialId));
    if (!Objects.equals(material.getOwnerSubject(), subject)) {
      throw new LearningIngestionAccessDeniedException(materialId);
    }
    return material;
  }

  private LearningIngestionJob loadOwnedJob(String materialId, String subject) {
    LearningIngestionJob job = learningIngestionJobRepository.findByMaterialId(materialId)
      .orElseThrow(() -> new LearningIngestionNotFoundException(materialId));
    if (!Objects.equals(job.getOwnerSubject(), subject)) {
      throw new LearningIngestionAccessDeniedException(materialId);
    }
    return job;
  }

  private List<String> existingChunkIds(String jobId) {
    return learningChunkRepository.findByJobIdOrderByChunkIndexAsc(jobId).stream()
      .map(LearningChunk::getId)
      .toList();
  }

  private LearningIngestionResponse toResponse(LearningIngestionJob job) {
    return toResponse(job, (int) learningChunkRepository.countByJobId(job.getId()));
  }

  private LearningIngestionResponse toResponse(LearningIngestionJob job, int chunkCount) {
    List<LearningChunk> chunks = learningChunkRepository.findByJobIdOrderByChunkIndexAsc(job.getId());
    long embeddingCount = learningEmbeddingRepository.countByChunkIdIn(chunks.stream().map(LearningChunk::getId).toList());
    return new LearningIngestionResponse(
      job.getId(),
      job.getMaterialId(),
      job.getStatus(),
      job.getRetryCount(),
      chunkCount,
      (int) embeddingCount,
      job.getLastError(),
      job.getCreatedAt(),
      job.getUpdatedAt()
    );
  }

  private String extractText(LearningMaterial material) {
    List<LearningMaterialSource> sources = learningMaterialSourceRepository.findByMaterialIdAndDeletedAtIsNullOrderByCreatedAtAsc(material.getId());
    if (!sources.isEmpty()) {
      String joinedSources = sources.stream()
        .map(source -> new String(source.getContent() == null ? new byte[0] : source.getContent(), StandardCharsets.UTF_8).trim())
        .filter(content -> !content.isBlank())
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");
      if (!joinedSources.isBlank()) {
        return joinedSources;
      }
    }

    if (material.getContent() == null || material.getContent().length == 0) {
      return material.getTitle() + "\n" + Objects.toString(material.getDescription(), "");
    }

    String text = new String(material.getContent(), StandardCharsets.UTF_8).trim();
    if (!text.isBlank()) {
      return text;
    }

    return material.getTitle() + "\n" + Objects.toString(material.getDescription(), "");
  }

  private static List<ChunkSlice> chunkText(String text) {
    String normalized = text == null ? "" : text.trim();
    if (normalized.isBlank()) {
      return List.of();
    }

    List<ChunkSlice> chunks = new ArrayList<>();
    int cursor = 0;
    for (String paragraph : CHUNK_BOUNDARY.split(normalized)) {
      if (paragraph == null || paragraph.isBlank()) {
        continue;
      }
      String remaining = paragraph.trim();
      int paragraphStart = normalized.indexOf(remaining, cursor);
      if (paragraphStart < 0) {
        paragraphStart = cursor;
      }
      cursor = paragraphStart + remaining.length();
      while (remaining.length() > CHUNK_SIZE) {
        String chunkContent = remaining.substring(0, CHUNK_SIZE);
        chunks.add(new ChunkSlice(chunkContent, paragraphStart, paragraphStart + CHUNK_SIZE));
        remaining = remaining.substring(CHUNK_SIZE).trim();
        paragraphStart += CHUNK_SIZE;
      }
      if (!remaining.isBlank()) {
        chunks.add(new ChunkSlice(remaining, paragraphStart, paragraphStart + remaining.length()));
      }
    }

    if (chunks.isEmpty()) {
      chunks.add(new ChunkSlice(normalized, 0, normalized.length()));
    }

    return chunks;
  }

  private record ChunkSlice(String content, int sourceStartOffset, int sourceEndOffset) {
  }

  private static int estimateTokens(String chunkContent) {
    return Math.max(1, chunkContent.length() / 4);
  }

  private static String failureMessage(Exception exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }
    return message.length() > 1000 ? message.substring(0, 1000) : message;
  }
}
