package com.example.PDF.chatbot.using.Spring.AI.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingService {

    private final VectorStore vectorStore;

    public void processPdf(MultipartFile file) throws IOException {
        log.info("PDF upload request received for file: {}", file.getOriginalFilename());
        log.info("Processing PDF file: {}", file.getOriginalFilename());

        String extractedText = extractTextFromPdf(file);
        log.info("Text extracted. Length: {}", extractedText.length());

        String cleanedText = cleanText(extractedText);
        log.info("Text cleaned. Length: {}", cleanedText.length());

        List<String> chunks = chunkText(cleanedText);
        log.info("Chunks created: {} chunks", chunks.size());

        List<Document> documents = chunks.stream()
                .map(Document::new)
                .toList();

        vectorStore.add(documents);
        log.info("Embeddings generated and stored for {} documents in vector store", documents.size());
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
        // This is a placeholder. The actual implementation depends on the VectorStore provider.
        // For PgVectorStore, you might need to truncate the table.
    }
}