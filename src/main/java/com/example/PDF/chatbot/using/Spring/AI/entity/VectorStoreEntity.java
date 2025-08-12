package com.example.PDF.chatbot.using.Spring.AI.entity;

import com.example.PDF.chatbot.using.Spring.AI.util.MetadataConverter;
import com.example.PDF.chatbot.using.Spring.AI.util.VectorConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "vector_store")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    @Convert(converter = MetadataConverter.class)
    private Map<String, Object> metadata;

    // Store as text representation and cast to vector on write
    @Column(name = "embedding", columnDefinition = "vector(384)")
    @ColumnTransformer(write = "?::vector")
    @Convert(converter = VectorConverter.class)
    private float[] embedding;

    @ManyToOne
    private UploadedFile uploadedFile;
} 