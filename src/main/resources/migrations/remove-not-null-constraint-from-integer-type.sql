-- liquibase formatted sql

-- changeset siddiq:1702991940923-1
ALTER TABLE integer_type
    ALTER COLUMN geq DROP NOT NULL;

-- changeset siddiq:1702991940923-2
ALTER TABLE integer_type
    ALTER COLUMN gt DROP NOT NULL;

-- changeset siddiq:1702991940923-3
ALTER TABLE integer_type
    ALTER COLUMN leq DROP NOT NULL;

-- changeset siddiq:1702991940923-4
ALTER TABLE integer_type
    ALTER COLUMN lt DROP NOT NULL;

