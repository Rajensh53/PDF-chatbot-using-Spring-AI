package com.example.PDF.chatbot.using.Spring.AI.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(name = "embedding_vector", columnDefinition = "vector(384)")
    private String embeddingVector;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.util.Date();
    }
} 