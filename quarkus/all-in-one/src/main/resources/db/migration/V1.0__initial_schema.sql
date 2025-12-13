CREATE TABLE Fruit
(
    id          UUID   NOT NULL,
    version     BIGINT NOT NULL,
    name        TEXT,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_fruit PRIMARY KEY (id)
);

ALTER TABLE Fruit
    ADD CONSTRAINT uc_fruit_name UNIQUE (name);