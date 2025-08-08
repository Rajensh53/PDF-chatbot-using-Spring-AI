CREATE EXTENSION IF NOT EXISTS vector;
DROP TABLE IF EXISTS document_chunks;
CREATE TABLE vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(384)
);
