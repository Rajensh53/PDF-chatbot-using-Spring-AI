package com.example.PDF.chatbot.using.Spring.AI.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimension}")
    private int dimension;

    /**
     * Configure the PgVectorStore with custom ONNX embedding adapter
     */
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, CustomOnnxEmbeddingClient embeddingModel) {
        return new PgVectorStore(jdbcTemplate, embeddingModel, dimension);
    }

    /**
     * Custom EmbeddingClient implementation that uses ONNX for embeddings
     */
    @Bean
    public CustomOnnxEmbeddingClient customOnnxEmbeddingClient() {
        return new CustomOnnxEmbeddingClient();
    }
}