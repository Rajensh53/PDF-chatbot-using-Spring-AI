package com.example.PDF.chatbot.using.Spring.AI.service;

import com.example.PDF.chatbot.using.Spring.AI.entity.UploadedFile;
import com.example.PDF.chatbot.using.Spring.AI.entity.VectorStoreEntity;
import com.example.PDF.chatbot.using.Spring.AI.repository.UploadedFileRepository;
import com.example.PDF.chatbot.using.Spring.AI.repository.VectorStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingService {

    private final VectorStore springAIVectorStore;
    private final UploadedFileRepository uploadedFileRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final EmbeddingModel embeddingModel;

    @Transactional
    public void processPdf(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("PDF upload request received for file: {}", fileName);

        // Compute content hash (SHA-256 of file bytes) to prevent duplicate uploads by renamed files
        String contentHash = sha256Hex(file.getBytes());

        // Duplicate by content check
        if (uploadedFileRepository.findByContentHash(contentHash).isPresent()) {
            throw new IllegalArgumentException("This file content is already uploaded (duplicate detected).");
        }

        log.info("Processing PDF file: {}", fileName);

        String extractedText = extractTextFromPdf(file);
        log.info("Text extracted. Length: {}", extractedText.length());

        String cleanedText = cleanText(extractedText);
        log.info("Text cleaned. Length: {}", cleanedText.length());

        List<String> chunks = chunkText(cleanedText);
        log.info("Chunks created: {} chunks", chunks.size());

        List<Document> documents = chunks.stream()
                .map(Document::new)
                .toList();

        // First save the uploaded file record
        UploadedFile uploadedFile = uploadedFileRepository.save(new UploadedFile(fileName, contentHash, Instant.now()));
        log.info("Saved uploaded file record with ID: {}", uploadedFile.getId());

        // Add to Spring AI vector store for semantic search
        springAIVectorStore.add(documents);
        log.info("Added {} documents to Spring AI vector store", documents.size());

        // Manually save each chunk with its embedding, linked to uploadedFile
        List<VectorStoreEntity> list = new ArrayList<>();
        for (Document document : documents) {
            List<Double> embeddingList = document.getEmbedding();
            if (embeddingList == null || embeddingList.isEmpty()) {
                embeddingList = embeddingModel.embed(document.getContent());
            }

            float[] embeddingArray = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embeddingArray[i] = embeddingList.get(i).floatValue();
            }

            VectorStoreEntity vectorStoreEntity = new VectorStoreEntity();
            vectorStoreEntity.setContent(document.getContent());
            vectorStoreEntity.setMetadata(document.getMetadata());
            vectorStoreEntity.setEmbedding(embeddingArray);
            vectorStoreEntity.setUploadedFile(uploadedFile);
            list.add(vectorStoreEntity);
        }

        vectorStoreRepository.saveAll(list);
        log.info("Successfully processed PDF: {} chunks saved with embeddings", chunks.size());
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String cleanText(String text) {
        return text
                .replaceAll("\\s+", " ")
                .replaceAll("\\n\\s*\\n", "\n")
                .trim();
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = 500;
        int overlap = 50;

        for (int i = 0; i < text.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end);

            if (!chunk.trim().isEmpty()) {
                chunks.add(chunk);
            }

            if (end == text.length()) {
                break;
            }
        }

        return chunks;
    }

    public void clearVectorStore() {
        log.info("Clearing all documents from vector store");
        // Clear both repositories
        vectorStoreRepository.deleteAll();
        uploadedFileRepository.deleteAll();
    }

    private static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}