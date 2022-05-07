CREATE TABLE Inbox_Xml_Message
(
    id               uuid      NOT NULL,
    occurred_at_utc  TIMESTAMP NOT NULL,
    processed_at_utc TIMESTAMP,
    type             text      NOT NULL,
    data             xml       NOT NULL
)
