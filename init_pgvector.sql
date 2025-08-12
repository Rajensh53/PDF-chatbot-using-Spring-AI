CREATE EXTENSION IF NOT EXISTS vector;
DROP TABLE IF EXISTS document_chunks;
CREATE TABLE vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(384)
);

-- Create an HNSW index for faster similarity search
CREATE INDEX ON vector_store USING HNSW (embedding vector_l2_ops);
