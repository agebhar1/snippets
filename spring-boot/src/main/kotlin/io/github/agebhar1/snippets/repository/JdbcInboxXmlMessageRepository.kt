package io.github.agebhar1.snippets.repository

import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.domain.InboxXmlMessageRepository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.xml.SqlXmlHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

@Service
class JdbcInboxXmlMessageRepository(
    private val jdbcClient: JdbcClient,
    private val sqlXmlHandler: SqlXmlHandler
) : InboxXmlMessageRepository {
    @Transactional
    override fun save(entity: InboxXmlMessage) {
        jdbcClient
            .sql("INSERT INTO INBOX_XML_MESSAGE VALUES(:id, :occurredAtUTC, null, null, :type, :data)")
            .param("id", entity.id)
            .param("occurredAtUTC", LocalDateTime.ofInstant(entity.occurredAtUtc, UTC))
            .param("type", entity.type)
            .param("data", sqlXmlHandler.newSqlXmlValue(entity.data))
            .update()
    }

    @Transactional
    override fun deleteById(id: UUID) {
        jdbcClient
            .sql("DELETE FROM INBOX_XML_MESSAGE WHERE id = :id")
            .param("id", id)
            .update()
    }
}
