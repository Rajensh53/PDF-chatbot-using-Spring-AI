package com.example.PDF.chatbot.using.Spring.AI.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"file_name"}),
    @UniqueConstraint(columnNames = {"content_hash"})
})
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash; // SHA-256 hex

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;



    public UploadedFile() {}

    public UploadedFile(String fileName, String contentHash, Instant uploadedAt) {
        this.fileName = fileName;
        this.contentHash = contentHash;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() { return id; }


}
