package com.example.PDF.chatbot.using.Spring.AI.service;

import com.example.PDF.chatbot.using.Spring.AI.entity.DocumentEntity;
import com.example.PDF.chatbot.using.Spring.AI.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final CustomOnnxService onnxService;
    private final DocumentRepository documentRepository;

    /**
     * Answer user query using RAG (Retrieval Augmented Generation)
     */
    public String answerQuery(String userQuery) {
        log.info("Processing user query: {}", userQuery);
        
        // 1. Search for relevant documents in database
        List<DocumentEntity> relevantDocuments = searchRelevantDocuments(userQuery);
        log.info("Found {} relevant documents", relevantDocuments.size());
        
        // 2. Create context from retrieved documents
        String context = createContextFromDocuments(relevantDocuments);
        
        // 3. Generate answer using ONNX model with context
        String answer = generateAnswer(userQuery, context);
        
        return answer;
    }

    /**
     * Search for relevant documents in database
     */
    private List<DocumentEntity> searchRelevantDocuments(String query) {
        try {
            // Generate embedding for the query
            List<Float> queryEmbedding = onnxService.generateEmbedding(query);
            String queryEmbeddingString = onnxService.embeddingToString(queryEmbedding);
            
            // Search for similar documents
            return documentRepository.findSimilarDocumentsWithThreshold(
                queryEmbeddingString, 0.8, 5);
                
        } catch (Exception e) {
            log.error("Error searching for relevant documents", e);
            return List.of();
        }
    }

    /**
     * Create context string from retrieved documents
     */
    private String createContextFromDocuments(List<DocumentEntity> documents) {
        if (documents.isEmpty()) {
            return "No relevant information found in the uploaded PDF.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Context from PDF:\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            DocumentEntity doc = documents.get(i);
            context.append("Document ").append(i + 1).append(":\n");
            context.append(doc.getContent()).append("\n\n");
        }
        
        return context.toString();
    }

    /**
     * Generate answer using ONNX model with context
     */
    private String generateAnswer(String userQuery, String context) {
        // Create prompt for ONNX model
        String prompt = String.format("""
            Context: %s
            
            Question: %s
            
            Answer based on the context above:""", context, userQuery);
        
        try {
            // Use our custom ONNX service for text generation
            String answer = onnxService.generateText(prompt);
            return answer;
        } catch (Exception e) {
            log.error("Error generating answer with ONNX model", e);
            // Fallback response if model fails
            if (context.contains("No relevant information")) {
                return "I don't have enough information to answer this question based on the provided PDF.";
            } else {
                return "Based on the PDF content: " + context.substring(0, Math.min(200, context.length())) + "...";
            }
        }
    }
} 