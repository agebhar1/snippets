DROP TABLE IF EXISTS Inbox_Xml_Message;

CREATE TABLE Inbox_Xml_Message
(
    id                        uuid      NOT NULL,
    occurred_at_utc           TIMESTAMP NOT NULL,
    processing_started_at_utc TIMESTAMP DEFAULT NULL,
    processing_started_by     text      DEFAULT NULL,
    type                      text      NOT NULL,
    data                      xml       NOT NULL,

    CONSTRAINT processing CHECK ((processing_started_at_utc IS NULL AND processing_started_by IS NULL) OR
                                 (processing_started_at_utc IS NOT NULL AND processing_started_by IS NOT NULL))
)
