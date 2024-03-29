package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestConstructor.AutowireMode.ALL
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.jdbc.JdbcTestUtils.countRowsInTableWhere

@JdbcTest
@TestConstructor(autowireMode = ALL)
class SpringIntegrationJdbcIntTest(
    val jdbcClient: JdbcClient,
    val messagingTemplate: MessagingTemplate
) {
    @Test
    @Sql("/sql/spring-integration-jdbc-int-test.sql")
    fun `message payload should be present in database table`() {
        val message = MessageBuilder.withPayload("hello").setHeader("externalId", 1).build()

        messagingTemplate.send(message)

        assertThat(countRowsInTableWhere(jdbcClient, "message", "payload = 'hello'")).isEqualTo(1)
    }

    @Test
    @Sql("/sql/spring-integration-jdbc-int-test.sql")
    fun `message payload should be updated in database table`() {
        val fstMessage = MessageBuilder.withPayload("hello").setHeader("externalId", 1).build()

        messagingTemplate.send(fstMessage)

        val sndMessage =
            MessageBuilder.withPayload("hello from jdbc").setHeader("externalId", 1).build()

        messagingTemplate.send(sndMessage)

        assertThat(countRowsInTableWhere(jdbcClient, "message", "payload = 'hello from jdbc'"))
            .isEqualTo(1)
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
                            VALUES (:headers[externalId], :headers[id], :payload)
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
