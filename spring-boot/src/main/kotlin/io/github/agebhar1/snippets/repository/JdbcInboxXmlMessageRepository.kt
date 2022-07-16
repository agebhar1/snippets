package io.github.agebhar1.snippets.repository

import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.domain.InboxXmlMessageRepository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.xml.SqlXmlHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

@Service
class JdbcInboxXmlMessageRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val sqlXmlHandler: SqlXmlHandler
) : InboxXmlMessageRepository {
    @Transactional
    override fun save(entity: InboxXmlMessage) {
        val parameterSource =
            MapSqlParameterSource(
                mapOf(
                    "id" to entity.id,
                    "occurredAtUTC" to LocalDateTime.ofInstant(entity.occurredAtUtc, UTC),
                    "type" to entity.type,
                    "data" to sqlXmlHandler.newSqlXmlValue(entity.data)))

        jdbcTemplate.update(
            "INSERT INTO INBOX_XML_MESSAGE VALUES(:id, :occurredAtUTC, null, null, :type, :data)",
            parameterSource)
    }

    @Transactional
    override fun deleteById(id: UUID) {
        jdbcTemplate.update(
            "DELETE FROM INBOX_XML_MESSAGE WHERE id = :id".trimIndent(),
            MapSqlParameterSource(mapOf("id" to id))
        )
    }
}
