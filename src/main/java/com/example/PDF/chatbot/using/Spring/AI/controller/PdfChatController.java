package com.example.PDF.chatbot.using.Spring.AI.controller;

import com.example.PDF.chatbot.using.Spring.AI.service.ChatService;
import com.example.PDF.chatbot.using.Spring.AI.service.PdfProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PdfChatController {

    private final PdfProcessingService pdfProcessingService;
    private final ChatService chatService;

    /**
     * Upload and process PDF file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPdf(@RequestParam("file") MultipartFile file) {
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
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        try {
            String userQuery = request.get("message");
            
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Message cannot be empty"));
            }

            // Get answer from chat service
            String answer = chatService.answerQuery(userQuery.trim());

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
    public ResponseEntity<Map<String, Object>> clearVectorStore() {
        try {
            pdfProcessingService.clearVectorStore();
            return ResponseEntity.ok(Map.of("message", "Vector store cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing vector store", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to clear vector store: " + e.getMessage()));
        }
    }
} 