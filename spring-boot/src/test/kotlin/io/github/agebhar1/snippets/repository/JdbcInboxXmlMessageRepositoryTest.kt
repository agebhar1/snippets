package io.github.agebhar1.snippets.repository

import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.toDocument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestConstructor.AutowireMode.ALL
import java.time.Instant
import java.util.UUID

@Import(JdbcInboxXmlMessageRepository::class, Jdbc4SqlXmlHandler::class)
@JdbcTest
@TestConstructor(autowireMode = ALL)
class JdbcInboxXmlMessageRepositoryTest(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val repository: JdbcInboxXmlMessageRepository
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
