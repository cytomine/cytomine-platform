
ALTER TABLE run_provisions
    DROP CONSTRAINT fkalebdf7rgjwqxhheamg4cxyxy;

DROP TABLE integer_provision CASCADE;

ALTER TABLE integer_type
    DROP COLUMN id;

