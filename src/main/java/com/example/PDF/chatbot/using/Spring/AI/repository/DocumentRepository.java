package com.example.PDF.chatbot.using.Spring.AI.repository;

import com.example.PDF.chatbot.using.Spring.AI.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    @Query(value = "SELECT *, embedding_vector <-> :queryEmbedding AS distance " +
                   "FROM document_chunks " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<DocumentEntity> findSimilarDocuments(@Param("queryEmbedding") String queryEmbedding, 
                                             @Param("limit") int limit);

    @Query(value = "SELECT *, embedding_vector <-> :queryEmbedding AS distance " +
                   "FROM document_chunks " +
                   "WHERE embedding_vector <-> :queryEmbedding < :threshold " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<DocumentEntity> findSimilarDocumentsWithThreshold(@Param("queryEmbedding") String queryEmbedding, 
                                                          @Param("threshold") double threshold,
                                                          @Param("limit") int limit);

    void deleteByFileName(String fileName);

    List<DocumentEntity> findByFileName(String fileName);
} 