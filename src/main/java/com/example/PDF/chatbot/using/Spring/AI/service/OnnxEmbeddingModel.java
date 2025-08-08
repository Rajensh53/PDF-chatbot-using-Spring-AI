package com.example.PDF.chatbot.using.Spring.AI.service;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OnnxEmbeddingModel implements EmbeddingModel {

    private final CustomOnnxService customOnnxService;

    public OnnxEmbeddingModel(CustomOnnxService customOnnxService) {
        this.customOnnxService = customOnnxService;
    }

    @Override
    public List<Double> embed(String text) {
        List<Float> floatEmbedding = customOnnxService.generateEmbedding(text);
        return floatEmbedding.stream().map(Float::doubleValue).collect(Collectors.toList());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = request.getInstructions().stream()
                .map(text -> new Embedding(embed(text), 0)) // Index 0 for simplicity, adjust if needed
                .collect(Collectors.toList());
        return new EmbeddingResponse(embeddings);
    }

    @Override
//    @Override
    public int dimensions() {
        // The database expects 384 dimensions, so we return that.
        return 384;
    }

        public List<Double> embed(Document document) {
        return embed(document.getContent());
    }
}
