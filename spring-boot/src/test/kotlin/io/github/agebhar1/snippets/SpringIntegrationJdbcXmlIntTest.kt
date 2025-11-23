package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.core.MessagingTemplate
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.jdbc.outbound.JdbcMessageHandler
import org.springframework.integration.support.MessageBuilder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler
import org.springframework.jdbc.support.xml.SqlXmlHandler
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestConstructor.AutowireMode.ALL
import org.w3c.dom.Document
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

@Import(Jdbc4SqlXmlHandler::class)
@JdbcTest
@TestConstructor(autowireMode = ALL)
class SpringIntegrationJdbcXmlIntTest(
    val jdbcClient: JdbcClient,
    val messagingTemplate: MessagingTemplate
) {
    @Test
    fun `message with XML payload should be present in database table`() {
        val message = MessageBuilder
            .withPayload("<some>data</some>".toDocument())
            .setHeader("externalId", UUID.fromString("99f4ade3-f6d1-49df-a52f-977e35f9a2cd"))
            .setHeader("occurredAtUTC", Instant.parse("2022-05-07T10:10:38Z"))
            .setHeader("type", "NONE")
            .build()

        messagingTemplate.send(message)

        val exists =
            jdbcClient.sql(
                """
                    SELECT id FROM INBOX_XML_MESSAGE
                    WHERE
                        occurred_at_utc = timestamp '2022-05-07 10:10:38' AND
                        processing_started_at_utc IS NULL AND
                        processing_started_by IS NULL AND
                        type = 'NONE' AND
                        data IS DOCUMENT
                """.trimIndent())
                .query(UUID::class.java)
                .list()

        assertThat(exists).containsExactly(UUID.fromString("99f4ade3-f6d1-49df-a52f-977e35f9a2cd"))
    }

    @EnableIntegration
    @TestConfiguration
    class Configuration {
        @Bean fun input() = DirectChannel()

        @Bean fun messagingTemplate() = MessagingTemplate(input())

        @Bean
        fun flow(jdbcTemplate: JdbcTemplate, sqlXmlHandler: SqlXmlHandler) =
            integrationFlow(input()) {
                handle(
                    JdbcMessageHandler(
                        jdbcTemplate,
                        """
                        INSERT INTO INBOX_XML_MESSAGE(id, occurred_at_utc, type, data)
                        VALUES (?, ?, ?, ?)
                        """.trimIndent()).apply {
                        setPreparedStatementSetter { ps, message ->
                            ps.setObject(1, message.headers["externalId"])
                            ps.setObject(2, LocalDateTime.ofInstant(message.headers["occurredAtUTC"] as Instant, UTC))
                            ps.setObject(3, message.headers["type"])
                            sqlXmlHandler.newSqlXmlValue(message.payload as Document).setValue(ps, 4)
                        }
                    }) {
                    id("jdbcHandler")
                }
            }
    }
}
