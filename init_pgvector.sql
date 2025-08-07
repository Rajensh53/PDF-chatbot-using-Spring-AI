CREATE EXTENSION IF NOT EXISTS vector;
DROP TABLE IF EXISTS document_chunks;
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    content TEXT,
    file_name VARCHAR(255),
    chunk_index INTEGER,
    total_chunks INTEGER,
    embedding_vector VECTOR(384),
    created_at TIMESTAMP
);

