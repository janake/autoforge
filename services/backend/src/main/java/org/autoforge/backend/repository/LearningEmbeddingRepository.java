package org.autoforge.backend.repository;

import org.autoforge.backend.domain.LearningEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningEmbeddingRepository extends JpaRepository<LearningEmbedding, String> {

  long countByChunkIdIn(Iterable<String> chunkIds);

  void deleteByChunkIdIn(Iterable<String> chunkIds);
}
