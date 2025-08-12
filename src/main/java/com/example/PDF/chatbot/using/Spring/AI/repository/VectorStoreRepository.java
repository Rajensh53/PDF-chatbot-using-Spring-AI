package com.example.PDF.chatbot.using.Spring.AI.repository;

import com.example.PDF.chatbot.using.Spring.AI.entity.VectorStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VectorStoreRepository extends JpaRepository<VectorStoreEntity, UUID> {
    
    @Query("SELECT v FROM VectorStoreEntity v WHERE v.uploadedFile.id = :uploadedFileId")
    List<VectorStoreEntity> findByUploadedFileId(@Param("uploadedFileId") UUID uploadedFileId);

    @Query("SELECT v FROM VectorStoreEntity v WHERE v.uploadedFile.fileName = :fileName")
    List<VectorStoreEntity> findByFileName(@Param("fileName") String fileName);

    void deleteByUploadedFileId(UUID uploadedFileId);
} 