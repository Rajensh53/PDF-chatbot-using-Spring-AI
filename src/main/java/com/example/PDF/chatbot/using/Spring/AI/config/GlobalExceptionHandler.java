package com.example.PDF.chatbot.using.Spring.AI.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Maximum upload size exceeded. Please upload a smaller file.")
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException ex) {
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        // Normalize message for frontend
        if (message != null && message.toLowerCase().contains("size")) {
            message = "Maximum upload size exceeded. Please upload a smaller file.";
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", message != null ? message : "Upload failed.")
        );
    }
}
