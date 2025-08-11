package com.example.PDF.chatbot.using.Spring.AI.repository;

import com.example.PDF.chatbot.using.Spring.AI.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    Optional<UploadedFile> findByFileName(String fileName);
}
