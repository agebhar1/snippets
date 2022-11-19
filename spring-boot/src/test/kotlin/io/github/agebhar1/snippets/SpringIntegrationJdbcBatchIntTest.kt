package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.core.MessagingTemplate
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.jdbc.JdbcMessageHandler
import org.springframework.integration.support.MessageBuilder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.jdbc.JdbcTestUtils.countRowsInTableWhere

import java.util.UUID.randomUUID

@AutoConfigureTestDatabase(replace = NONE)
@JdbcTest(properties = ["spring.datasource.url=jdbc:tc:postgresql:14.6:///"])
class SpringIntegrationJdbcBatchIntTest(
    @Autowired val jdbcTemplate: JdbcTemplate,
    @Autowired val messagingTemplate: MessagingTemplate
) {
    @Test
    @Sql("/sql/spring-integration-jdbc-int-test.sql")
    fun `list of tuples should be present in database table`() {
        val messageId = randomUUID()

        val message =
            MessageBuilder.withPayload(
                listOf(listOf(1, "1st message"), listOf(2, "2nd message"), listOf(3, "3rd message")))
                .setHeader("messageId", messageId)
                .build()

        messagingTemplate.send(message)

        assertThat(countRowsInTableWhere(jdbcTemplate, "message", "messageId = '$messageId'"))
            .isEqualTo(3)
    }

    @Test
    @Sql("/sql/spring-integration-jdbc-int-test.sql")
    fun `list of tuples should be updated in database table`() {
        val messageId = randomUUID()

        val fstMessage =
            MessageBuilder.withPayload(
                listOf(listOf(1, "1st message"), listOf(2, "2nd message"), listOf(3, "3rd message")))
                .setHeader("messageId", messageId)
                .build()

        messagingTemplate.send(fstMessage)

        val sndMessage =
            MessageBuilder.withPayload(
                listOf(listOf(1, "message one"), listOf(2, "message two"), listOf(3, "message three")))
                .setHeader("messageId", messageId)
                .build()

        messagingTemplate.send(sndMessage)

        assertThat(
            countRowsInTableWhere(
                jdbcTemplate, "message", "payload IN ('message one', 'message two', 'message three')"))
            .isEqualTo(3)
    }

    @EnableIntegration
    @TestConfiguration
    class Configuration {
        @Bean fun input() = DirectChannel()

        @Bean fun messagingTemplate() = MessagingTemplate(input())

        @Bean
        fun flow(jdbcTemplate: JdbcTemplate) =
            integrationFlow(input()) {
                handle(
                    JdbcMessageHandler(
                        jdbcTemplate,
                        """
                  INSERT INTO message(id, messageId, payload)
                  VALUES (:payload[0], :headers[messageId], :payload[1])
                  ON CONFLICT (id)
                  DO UPDATE SET
                    messageId = EXCLUDED.messageId,
                    payload = EXCLUDED.payload
                  """)) {
                    id("jdbcHandler")
                }
            }
    }
}
