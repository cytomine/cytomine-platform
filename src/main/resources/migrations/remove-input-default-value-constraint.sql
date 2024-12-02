-- liquibase formatted sql

-- changeset bale:1816435112000-1
ALTER TABLE input
    ALTER COLUMN default_value DROP NOT NULL;
