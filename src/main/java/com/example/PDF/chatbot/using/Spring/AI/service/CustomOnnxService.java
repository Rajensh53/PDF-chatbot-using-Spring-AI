package com.example.PDF.chatbot.using.Spring.AI.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ai.onnxruntime.*;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.model.ChatResponse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class CustomOnnxService {

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;
    private boolean modelsLoaded = false;
    
    @Autowired
    private ChatClient chatClient;

    public CustomOnnxService() {
        try {
            initializeModels();
        } catch (Exception e) {
            log.error("Failed to initialize ONNX models", e);
            throw new RuntimeException("ONNX model or tokenizer not loaded. Cannot generate embeddings.");
        }
    }

    private void initializeModels() throws Exception {
        log.info("Initializing ONNX models...");
        // Use absolute paths to avoid issues with backslashes in Windows paths
        Path modelPath = Paths.get("onnx-output-folder", "model.onnx").toAbsolutePath();
        Path tokenizerPath = Paths.get("onnx-output-folder", "tokenizer.json").toAbsolutePath();
        
        log.info("Model path: {}", modelPath);
        log.info("Tokenizer path: {}", tokenizerPath);
        
        if (modelPath.toFile().exists() && tokenizerPath.toFile().exists()) {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            
            // Use the tokenizer path as a string with forward slashes
            String tokenizerPathStr = tokenizerPath.toString().replace("\\", "/");
            log.info("Using tokenizer path: {}", tokenizerPathStr);
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPathStr);
            
            modelsLoaded = true;
            log.info("✅ ONNX model and tokenizer loaded.");
        } else {
            throw new IllegalStateException("ONNX model or tokenizer not found. Embedding generation will not work.");
        }
    }

    public List<Float> generateEmbedding(String text) {
        if (!modelsLoaded) {
            throw new IllegalStateException("ONNX model or tokenizer not loaded. Cannot generate embeddings.");
        }
        try {
            // Tokenize input
            var encoding = tokenizer.encode(text);
            long[] inputIds = convertToLongArray(encoding.getIds());
            long[] attentionMask = convertToLongArray(encoding.getAttentionMask());

            // Prepare input tensors
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, new long[][]{inputIds});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, new long[][]{attentionMask});

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            // Run inference
            OrtSession.Result result = session.run(inputs);

            // Extract [CLS] embedding (first token)
            float[][][] embeddings = (float[][][]) result.get(0).getValue();
            float[] clsEmbedding = embeddings[0][0];

            List<Float> embeddingList = new ArrayList<>();
            for (float f : clsEmbedding) embeddingList.add(f);

            // Clean up
            inputIdsTensor.close();
            attentionMaskTensor.close();
            result.close();

            return embeddingList;
        } catch (Exception e) {
            log.error("Error generating embedding with ONNX model", e);
            throw new RuntimeException("Failed to generate embedding with ONNX model", e);
        }
    }

    public String embeddingToString(List<Float> embedding) {
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
        log.info("Cleaning up ONNX service resources");
    }
    
    /**
     * Helper method to convert tokenizer output to long array
     * Handles different possible return types from the tokenizer
     */
    private long[] convertToLongArray(Object obj) {
        if (obj instanceof Long[]) {
            return Arrays.stream((Long[]) obj).mapToLong(i -> i).toArray();
        } else if (obj instanceof long[]) {
            return (long[]) obj;
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            long[] result = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Number) {
                    result[i] = ((Number) item).longValue();
                } else {
                    throw new IllegalArgumentException("Unsupported list item type: " + item.getClass());
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + obj.getClass());
        }
    }
    
    /**
     * Generate text using AI model with the given prompt
     * This method uses Spring AI's ChatClient to generate text responses
     * 
     * @param prompt The prompt to generate text from
     * @return The generated text response
     */
    public String generateText(String prompt) {
        log.info("Generating text with prompt: {}", prompt);
        try {
            // Use Spring AI's ChatClient to generate text
            String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            log.info("Generated text successfully");
            return content;
        } catch (Exception e) {
            log.error("Error generating text with AI model", e);
            throw new RuntimeException("Failed to generate text with AI model", e);
        }
    }
}