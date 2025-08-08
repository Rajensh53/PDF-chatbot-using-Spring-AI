package com.example.PDF.chatbot.using.Spring.AI.service;

import ai.onnxruntime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.LongBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class CustomOnnxService {

    private OrtEnvironment environment;
    private OrtSession session;
    private Map<String, Integer> tokenizerVocab;
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
        environment = OrtEnvironment.getEnvironment();

        Path modelPath = Paths.get("onnx-output-folder", "model.onnx");
        Path tokenizerPath = Paths.get("onnx-output-folder", "tokenizer.json");

        if (modelPath.toFile().exists() && tokenizerPath.toFile().exists()) {
            session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            log.info("✅ ONNX model loaded from: {}", modelPath);

            // Load tokenizer vocabulary
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> tokenizerJson = mapper.readValue(tokenizerPath.toFile(), Map.class);
            tokenizerVocab = (Map<String, Integer>) ((Map<String, Object>) tokenizerJson.get("model")).get("vocab");
            log.info("✅ Tokenizer loaded from: {}", tokenizerPath);

            modelsLoaded = true;
        } else {
            log.warn("⚠️ ONNX model or tokenizer not found. Using fallback embedding generation.");
            modelsLoaded = false;
        }
    }

    public List<Float> generateEmbedding(String text) {
        if (!modelsLoaded) {
            throw new IllegalStateException("ONNX models are not loaded. Cannot generate embedding.");
        }

        try {
            // 1. Tokenize text
            List<String> tokens = tokenize(text.toLowerCase());
            int[] tokenIds = tokens.stream().mapToInt(t -> tokenizerVocab.getOrDefault(t, tokenizerVocab.get("[UNK]"))).toArray();

            // 2. Create input tensor
            long[] inputIds = Arrays.stream(tokenIds).mapToLong(i -> i).toArray();
            long[] attentionMask = new long[inputIds.length];
            Arrays.fill(attentionMask, 1);

            long[] shape = {1, inputIds.length};
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(inputIds), shape);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), shape);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            // 3. Run inference
            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                return meanPooling(output, attentionMask);
            }

        } catch (OrtException e) {
            log.error("Error during ONNX model inference", e);
            throw new RuntimeException("Failed to generate embedding due to ONNX inference error.", e);
        } catch (Exception e) {
            log.error("Unexpected error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding due to an unexpected error.", e);
        }
    }

    private List<String> tokenize(String text) {
        // Simple whitespace tokenizer
        return Arrays.asList(text.split("\s+"));
    }

    private List<Float> meanPooling(float[][][] embeddings, long[] attentionMask) {
        List<Float> meanPooled = new ArrayList<>();
        int numTokens = embeddings[0].length;
        int embeddingDim = embeddings[0][0].length;

        for (int i = 0; i < embeddingDim; i++) {
            float sum = 0.0f;
            int count = 0;
            for (int j = 0; j < numTokens; j++) {
                if (attentionMask[j] == 1) {
                    sum += embeddings[0][j][i];
                    count++;
                }
            }
            meanPooled.add(sum / count);
        }
        return meanPooled;
    }

    private List<Float> generateDummyEmbedding(String text) {
        throw new UnsupportedOperationException("Dummy embedding generation is not allowed.");
    }


    public String generateText(String prompt) {
        throw new UnsupportedOperationException("Text generation is not supported without a loaded model.");
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
        String clean = embeddingString.replace("[", "").replace("]", "");
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