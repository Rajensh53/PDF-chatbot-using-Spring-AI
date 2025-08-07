package com.example.PDF.chatbot.using.Spring.AI.service;

import com.example.PDF.chatbot.using.Spring.AI.entity.DocumentEntity;
import com.example.PDF.chatbot.using.Spring.AI.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final CustomOnnxService onnxService;
    private final DocumentRepository documentRepository;
    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    private final String PROMPT_TEMPLATE = """
            You are a helpful assistant that answers questions based on the provided documents.
            Use only the information from the documents to answer the question.
            If you don't know the answer, say that you don't know.
            
            QUESTION:
            {input}
            
            DOCUMENTS:
            {documents}
            """;

    /**
     * Answer user query using RAG (Retrieval Augmented Generation)
     */
    public String answerQuery(String userQuery) {
        log.info("Processing user query: {}", userQuery);
        
        try {
            // Search for relevant documents
            List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.query(userQuery)
                    .withTopK(5)
                    .build()
            );

            // Prepare prompt template and parameters
            PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("input", userQuery);
            promptParameters.put("documents", relevantDocs.stream()
                    .map(doc -> doc.getContent())
                    .reduce("", (a, b) -> a + "\n" + b));

            // Generate response using the chat model
            String answer = chatModel.call(promptTemplate.create(promptParameters))
                    .getResult()
                    .getOutput()
                    .getContent();

            log.info("Generated answer using RAG");
            return answer;

        } catch (Exception e) {
            log.error("Error generating answer with RAG", e);
            return "I encountered an error while trying to answer your question. Please try again later.";
        }
    }

    /**
     * Search for relevant documents using ONNX embeddings
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
}