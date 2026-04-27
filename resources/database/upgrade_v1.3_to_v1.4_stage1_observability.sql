ALTER TABLE t_knowledge_document
    ADD COLUMN IF NOT EXISTS parse_engine VARCHAR(32),
    ADD COLUMN IF NOT EXISTS chunk_engine VARCHAR(32);

ALTER TABLE t_knowledge_document_chunk_log
    ADD COLUMN IF NOT EXISTS parse_engine VARCHAR(32),
    ADD COLUMN IF NOT EXISTS chunk_engine VARCHAR(32);
