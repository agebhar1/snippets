package io.github.agebhar1.snippets.repository

import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.toDocument

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler

import java.time.Instant
import java.util.UUID

@AutoConfigureTestDatabase(replace = NONE)
@Import(JdbcInboxXmlMessageRepository::class, Jdbc4SqlXmlHandler::class)
@JdbcTest(properties = ["spring.datasource.url=jdbc:tc:postgresql:14.5:///"])
class JdbcInboxXmlMessageRepositoryTest(
    @Autowired private val jdbcTemplate: NamedParameterJdbcTemplate,
    @Autowired private val repository: JdbcInboxXmlMessageRepository
) {
    @Test
    fun `should save entity`() {
        val entity =
            InboxXmlMessage(
                id = UUID.fromString("99f4ade3-f6d1-49df-a52f-977e35f9a2cd"),
                occurredAtUtc = Instant.parse("2022-05-07T10:10:38Z"),
                type = "NONE",
                data = "<some>data</some>".toDocument())

        repository.save(entity)

        val exists =
            jdbcTemplate.queryForList(
                """
                    SELECT id FROM INBOX_XML_MESSAGE
                    WHERE
                        occurred_at_utc = timestamp '2022-05-07 10:10:38' AND
                        processing_started_at_utc IS NULL AND
                        processing_started_by IS NULL AND
                        type = 'NONE' AND
                        data IS DOCUMENT
                """.trimIndent(),
                emptyMap<String, Any>(),
                UUID::class.java)

        assertThat(exists).containsExactly(UUID.fromString("99f4ade3-f6d1-49df-a52f-977e35f9a2cd"))
    }

    @Test
    fun `should delete entity by Id`() {
        val id = UUID.fromString("99f4ade3-f6d1-49df-a52f-977e35f9a2cd")
        val entity =
            InboxXmlMessage(
                id,
                occurredAtUtc = Instant.parse("2022-05-07T10:10:38Z"),
                type = "NONE",
                data = "<some>data</some>".toDocument())

        repository.save(entity)
        repository.deleteById(id)

        val exists = jdbcTemplate.queryForObject(
            """
                SELECT count(*) = 1 FROM INBOX_XML_MESSAGE
                WHERE id = :id
            """.trimIndent(),
            mapOf("id" to id),
            Boolean::class.java)
        assertThat(exists).isFalse
    }
}
