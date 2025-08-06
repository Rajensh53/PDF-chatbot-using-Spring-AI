package com.example.PDF.chatbot.using.Spring.AI.service;

import com.example.PDF.chatbot.using.Spring.AI.entity.DocumentEntity;
import com.example.PDF.chatbot.using.Spring.AI.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingService {

    private final CustomOnnxService onnxService;
    private final DocumentRepository documentRepository;

    /**
     * Process PDF file: Extract → Clean → Chunk → Embed → Store
     */
    public void processPdf(MultipartFile file) throws IOException {
        log.info("Processing PDF file: {}", file.getOriginalFilename());
        
        // 1. Extract text from PDF
        String extractedText = extractTextFromPdf(file);
        log.info("Extracted text length: {}", extractedText.length());
        
        // 2. Clean text
        String cleanedText = cleanText(extractedText);
        log.info("Cleaned text length: {}", cleanedText.length());
        
        // 3. Chunk text
        List<String> chunks = chunkText(cleanedText);
        log.info("Created {} chunks", chunks.size());
        
        // 4. Create documents and embed
        List<DocumentEntity> documents = createDocuments(chunks, file.getOriginalFilename());
        
        // 5. Store in database
        documentRepository.saveAll(documents);
        log.info("Stored {} documents in database", documents.size());
    }

    /**
     * 1. Extract text from PDF using Apache PDFBox
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 2. Clean text - remove extra whitespace and format
     */
    private String cleanText(String text) {
        return text
                .replaceAll("\\s+", " ")  // Replace multiple whitespace with single space
                .replaceAll("\\n\\s*\\n", "\n")  // Remove empty lines
                .trim();
    }

    /**
     * 3. Split text into 500-character chunks with overlap
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = 500;
        int overlap = 50;
        
        for (int i = 0; i < text.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end);
            
            // Don't add empty chunks
            if (!chunk.trim().isEmpty()) {
                chunks.add(chunk);
            }
            
            // If we've reached the end, break
            if (end == text.length()) {
                break;
            }
        }
        
        return chunks;
    }

    /**
     * 4. Create Document entities with metadata and embeddings
     */
    private List<DocumentEntity> createDocuments(List<String> chunks, String fileName) {
        List<DocumentEntity> documents = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            
            // Generate embedding using our custom ONNX service
            List<Float> embedding = onnxService.generateEmbedding(chunk);
            String embeddingString = onnxService.embeddingToString(embedding);
            
            DocumentEntity document = new DocumentEntity();
            document.setContent(chunk);
            document.setFileName(fileName);
            document.setChunkIndex(i);
            document.setTotalChunks(chunks.size());
            document.setEmbeddingVector(embeddingString);
            
            documents.add(document);
        }
        
        return documents;
    }

    /**
     * Clear all documents from database
     */
    public void clearVectorStore() {
        log.info("Clearing all documents from database");
        documentRepository.deleteAll();
    }

    /**
     * Clear documents for a specific file
     */
    public void clearDocumentsForFile(String fileName) {
        log.info("Clearing documents for file: {}", fileName);
        documentRepository.deleteByFileName(fileName);
    }
} 