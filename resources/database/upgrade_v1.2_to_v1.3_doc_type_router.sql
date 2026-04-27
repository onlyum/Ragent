ALTER TABLE t_knowledge_document
    ADD COLUMN IF NOT EXISTS doc_type VARCHAR(32) NOT NULL DEFAULT 'general';

UPDATE t_knowledge_document
SET doc_type = 'general'
WHERE doc_type IS NULL OR btrim(doc_type) = '';

ALTER TABLE t_knowledge_document_chunk_log
    ADD COLUMN IF NOT EXISTS doc_type VARCHAR(32);

ALTER TABLE t_knowledge_document
    ADD COLUMN IF NOT EXISTS parse_engine VARCHAR(32),
    ADD COLUMN IF NOT EXISTS chunk_engine VARCHAR(32);

ALTER TABLE t_knowledge_document_chunk_log
    ADD COLUMN IF NOT EXISTS parse_engine VARCHAR(32),
    ADD COLUMN IF NOT EXISTS chunk_engine VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_doc_type ON t_knowledge_document (doc_type);
