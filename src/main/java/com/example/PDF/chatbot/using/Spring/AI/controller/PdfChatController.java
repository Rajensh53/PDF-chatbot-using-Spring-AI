package com.example.PDF.chatbot.using.Spring.AI.controller;

import com.example.PDF.chatbot.using.Spring.AI.service.ChatService;
import com.example.PDF.chatbot.using.Spring.AI.service.PdfProcessingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PdfChatController {

    private final PdfProcessingService pdfProcessingService;
    private final ChatService chatService;

    private static final String CHAT_HISTORY_SESSION_KEY = "chatHistory";

    /**
     * Upload and process PDF file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPdf(@RequestParam("file") MultipartFile file, HttpSession session) {
        log.info("PDF upload request received for file: {}", file.getOriginalFilename());
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please select a file to upload"));
            }

            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only PDF files are allowed"));
            }

            // Process PDF
            pdfProcessingService.processPdf(file);

            // Clear chat history on new PDF upload
            session.removeAttribute(CHAT_HISTORY_SESSION_KEY);

            return ResponseEntity.ok(Map.of(
                    "message", "PDF uploaded and processed successfully",
                    "fileName", file.getOriginalFilename(),
                    "fileSize", file.getSize()
            ));

        } catch (IOException e) {
            log.error("Error processing PDF file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process PDF file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during PDF processing", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * Chat with the PDF content
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request, HttpSession session) {
        log.info("Chat request received.");
        try {
            String userQuery = request.get("message");
            
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Message cannot be empty"));
            }

            // Retrieve or initialize chat history
            List<Message> chatHistory = (List<Message>) session.getAttribute(CHAT_HISTORY_SESSION_KEY);
            if (chatHistory == null) {
                chatHistory = new ArrayList<>();
            }

            // Add user message to history
            UserMessage userMessage = new UserMessage(userQuery.trim());
            chatHistory.add(userMessage);

            // Get answer from chat service, passing the history
            String answer = chatService.answerQuery(userQuery.trim(), chatHistory);

            // Add assistant message to history
            AssistantMessage assistantMessage = new AssistantMessage(answer);
            chatHistory.add(assistantMessage);

            // Store updated history in session
            session.setAttribute(CHAT_HISTORY_SESSION_KEY, chatHistory);

            return ResponseEntity.ok(Map.of(
                    "answer", answer,
                    "question", userQuery
            ));

        } catch (Exception e) {
            log.error("Error during chat", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process chat request: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "PDF Chatbot is running"
        ));
    }

    /**
     * Clear vector store (for testing purposes)
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearVectorStore(HttpSession session) {
        try {
            pdfProcessingService.clearVectorStore();
            session.removeAttribute(CHAT_HISTORY_SESSION_KEY); // Clear chat history on store clear
            return ResponseEntity.ok(Map.of("message", "Vector store cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing vector store", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to clear vector store: " + e.getMessage()));
        }
    }
} 