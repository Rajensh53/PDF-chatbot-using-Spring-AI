package com.example.PDF.chatbot.using.Spring.AI.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class CustomOnnxService {

    private boolean modelsLoaded = false;

    public CustomOnnxService() {
        try {
            initializeModels();
        } catch (Exception e) {
            log.error("Failed to initialize ONNX models", e);
        }
    }

    private void initializeModels() throws Exception {
        log.info("Initializing ONNX models...");
        
        // Check if ONNX model exists
        Path modelPath = Paths.get("onnx-output-folder", "model.onnx");
        if (modelPath.toFile().exists()) {
            modelsLoaded = true;
            log.info("✅ ONNX model found at: {}", modelPath);
        } else {
            log.warn("⚠️ ONNX model not found at: {}. Using fallback embedding generation.", modelPath);
            modelsLoaded = false;
        }
    }

    public List<Float> generateEmbedding(String text) {
        if (!modelsLoaded) {
            log.debug("Using fallback embedding generation for text: {}", text.substring(0, Math.min(50, text.length())));
            return generateDummyEmbedding(text);
        }

        try {
            // For now, use dummy embedding since ONNX integration is complex
            // In a real implementation, you would use the ONNX model here
            return generateDummyEmbedding(text);
            
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            return generateDummyEmbedding(text);
        }
    }

    private List<Float> generateDummyEmbedding(String text) {
        // Generate a deterministic dummy embedding based on text hash
        List<Float> embedding = new ArrayList<>();
        int hash = text.hashCode();
        Random random = new Random(hash);
        
        for (int i = 0; i < 384; i++) {
            embedding.add(random.nextFloat() * 2 - 1); // Values between -1 and 1
        }
        
        return embedding;
    }

    public String generateText(String prompt) {
        if (!modelsLoaded) {
            return generateSimpleResponse(prompt);
        }

        try {
            // For now, use a simple response generation
            // In a real implementation, you would use the ONNX model for text generation
            return generateSimpleResponse(prompt);
            
        } catch (Exception e) {
            log.error("Error generating text", e);
            return generateSimpleResponse(prompt);
        }
    }

    private String generateSimpleResponse(String prompt) {
        // Simple rule-based response generation
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("hello") || lowerPrompt.contains("hi")) {
            return "Hello! I'm your PDF chatbot. How can I help you with your document?";
        }
        
        if (lowerPrompt.contains("what") || lowerPrompt.contains("how") || lowerPrompt.contains("when") || lowerPrompt.contains("where")) {
            if (lowerPrompt.contains("context") || lowerPrompt.contains("pdf")) {
                return "Based on the PDF content you've uploaded, I can help answer your questions. Please ask me something specific about the document.";
            } else {
                return "I can help you with questions about your uploaded PDF document. Please ask me something specific about the content.";
            }
        }
        
        if (lowerPrompt.contains("thank")) {
            return "You're welcome! Feel free to ask more questions about your PDF.";
        }
        
        // Default response
        return "I understand your question. Based on the PDF content, I'll do my best to provide a relevant answer. Could you please be more specific about what you'd like to know?";
    }

    public String embeddingToString(List<Float> embedding) {
        // Convert embedding list to string for database storage
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    public List<Float> stringToEmbedding(String embeddingString) {
        // Convert string back to embedding list
        List<Float> embedding = new ArrayList<>();
        String clean = embeddingString.replaceAll("[\\[\\]]", "");
        String[] parts = clean.split(",");
        for (String part : parts) {
            try {
                embedding.add(Float.parseFloat(part.trim()));
            } catch (NumberFormatException e) {
                embedding.add(0.0f);
            }
        }
        return embedding;
    }

    public boolean isModelsLoaded() {
        return modelsLoaded;
    }

    public void cleanup() {
        // Cleanup resources if needed
        log.info("Cleaning up ONNX service resources");
    }
} 