package com.example.PDF.chatbot.using.Spring.AI.config;

import com.example.PDF.chatbot.using.Spring.AI.service.CustomOnnxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom implementation of EmbeddingModel that uses ONNX model for embeddings
 */
@Slf4j
public class CustomOnnxEmbeddingClient implements EmbeddingModel {

    @Autowired
    private CustomOnnxService onnxService;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        log.info("Generating embeddings for {} texts", request.getInputs().size());
        List<Embedding> embeddings = request.getInputs().stream()
                .map(this::generateEmbedding)
                .collect(Collectors.toList());
        return new EmbeddingResponse(embeddings);
    }
    
    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        Assert.notNull(texts, "Texts must not be null");
        return call(new EmbeddingRequest(texts, null));
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Assert.notNull(texts, "Texts must not be null");
        log.info("Generating embeddings for {} texts", texts.size());
        return texts.stream()
                .map(this::generateEmbeddingAsFloatArray)
                .collect(Collectors.toList());
    }

    @Override
    public float[] embed(String text) {
        Assert.notNull(text, "Text must not be null");
        log.info("Generating embedding for text");
        return generateEmbeddingAsFloatArray(text);
    }

    @Override
    public float[] embed(Document document) {
        Assert.notNull(document, "Document must not be null");
        log.info("Generating embedding for document");
        return generateEmbeddingAsFloatArray(document.getContent());
    }
    
    @Override
    public int dimensions() {
        // Return the dimensionality of the embedding vectors
        return embed("Test String").length;
    }

    /**
     * Generate embedding using ONNX model and convert to Embedding object
     */
    private Embedding generateEmbedding(String text) {
        List<Float> floatEmbedding = onnxService.generateEmbedding(text);
        float[] embedding = floatEmbedding.stream()
                .toArray(Float[]::new);
        // Convert Float[] to float[] primitive array
        float[] primitiveEmbedding = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            primitiveEmbedding[i] = embedding[i];
        }
        return new Embedding(primitiveEmbedding);
    }

    /**
     * Generate embedding using ONNX model and convert to float array
     */
    private float[] generateEmbeddingAsFloatArray(String text) {
        List<Float> floatEmbedding = onnxService.generateEmbedding(text);
        float[] embedding = new float[floatEmbedding.size()];
        for (int i = 0; i < floatEmbedding.size(); i++) {
            embedding[i] = floatEmbedding.get(i);
        }
        return embedding;
    }
}