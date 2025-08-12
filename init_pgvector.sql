CREATE EXTENSION IF NOT EXISTS vector;
DROP TABLE IF EXISTS document_chunks;
DROP TABLE IF EXISTS vector_store CASCADE;
DROP TABLE IF EXISTS uploaded_files CASCADE;

-- Create uploaded_files table first
CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_file_name UNIQUE (file_name),
    CONSTRAINT uk_content_hash UNIQUE (content_hash)
);

-- Create vector_store table with reference to uploaded_files
CREATE TABLE vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding VECTOR(384),
    uploaded_file_id UUID,
    CONSTRAINT fk_uploaded_file FOREIGN KEY (uploaded_file_id) REFERENCES uploaded_files(id) ON DELETE CASCADE
);

-- Create index for better performance
CREATE INDEX idx_vector_store_uploaded_file ON vector_store(uploaded_file_id);
