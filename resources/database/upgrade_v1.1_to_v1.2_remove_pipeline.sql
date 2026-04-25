ALTER TABLE t_knowledge_document
    DROP COLUMN IF EXISTS process_mode,
    DROP COLUMN IF EXISTS pipeline_id;

ALTER TABLE t_knowledge_document_chunk_log
    DROP COLUMN IF EXISTS process_mode,
    DROP COLUMN IF EXISTS pipeline_id;

DROP TABLE IF EXISTS t_ingestion_task_node;
DROP TABLE IF EXISTS t_ingestion_task;
DROP TABLE IF EXISTS t_ingestion_pipeline_node;
DROP TABLE IF EXISTS t_ingestion_pipeline;
