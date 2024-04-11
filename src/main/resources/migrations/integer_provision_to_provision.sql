-- liquibase formatted sql

-- changeset siddiq:1712065306788-1
ALTER TABLE run_provisions
    DROP CONSTRAINT fkalebdf7rgjwqxhheamg4cxyxy;

-- changeset siddiq:1712065306788-4
DROP TABLE integer_provision CASCADE;

-- changeset siddiq:1712065306788-5
ALTER TABLE integer_type
    DROP COLUMN id;

