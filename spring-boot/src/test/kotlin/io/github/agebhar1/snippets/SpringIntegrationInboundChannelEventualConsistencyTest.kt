package io.github.agebhar1.snippets

import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.domain.InboxXmlMessageRepository
import io.github.agebhar1.snippets.repository.JdbcInboxXmlMessageRepository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.core.MessagingTemplate
import org.springframework.integration.support.MessageBuilder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.IdGenerator
import org.w3c.dom.Document

import java.lang.IllegalStateException
import java.time.Clock
import java.time.Clock.fixed
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.util.UUID

@Service
@Suppress("CLASS_NAME_INCORRECT")
class JustAService(
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val repository: InboxXmlMessageRepository,
) {
    @ServiceActivator(inputChannel = "inbound")
    @Transactional(propagation = MANDATORY)
    fun handle(@Payload document: Document, headers: MessageHeaders) {
        val entity =
            InboxXmlMessage(
                id = idGenerator.generateId(),
                occurredAtUtc = Instant.now(clock),
                type = "single",
                data = document)

        repository.save(entity)
    }
}

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@Import(value = [JdbcInboxXmlMessageRepository::class, JustAService::class])
@JdbcTest
@Sql(
    executionPhase = AFTER_TEST_METHOD,
    statements = ["DELETE FROM INBOX_XML_MESSAGE WHERE id = '4bafe8fd-2086-4abb-a79f-47bbaa0aa4c9'"])
@Transactional(propagation = NEVER)
class SpringIntegrationInboundChannelEventualConsistencyTest(
    @Autowired val txTemplate: TransactionTemplate,
    @Autowired val jdbcTemplate: JdbcTemplate,
    @Autowired private val messagingTemplate: MessagingTemplate
) : AbstractPostgreSQLContainerIntTest() {
    @Test
    fun `should save message from channel to inbox database table and proceed normally`() {
        val message = MessageBuilder.withPayload("<some>data</some>".toDocument()).build()

        txTemplate.execute { messagingTemplate.send(message) }

        val actual =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INBOX_XML_MESSAGE WHERE id = '4bafe8fd-2086-4abb-a79f-47bbaa0aa4c9'",
                Long::class.java)
        assertThat(actual).isEqualTo(1)
    }

    @Test
    fun `should not save message from channel to inbox database table if exception occurs after repository invocation`() {
        val message = MessageBuilder.withPayload("<some>data</some>".toDocument()).build()

        assertThrows<IllegalStateException> {
            txTemplate.execute<Nothing> {
                messagingTemplate.send(message)
                throw IllegalStateException()
            }
        }

        val actual =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INBOX_XML_MESSAGE", Long::class.java)
        assertThat(actual).isEqualTo(0)
    }

    @EnableIntegration
    @TestConfiguration
    class Configuration {
        @Bean fun clock(): Clock = fixed(Instant.parse("2022-05-07T14:05:00Z"), UTC)

        @Bean
        fun idGenerator() = IdGenerator { UUID.fromString("4bafe8fd-2086-4abb-a79f-47bbaa0aa4c9") }

        @Bean fun messagingTemplate() = MessagingTemplate(inbound())

        @Bean fun inbound() = DirectChannel()

        @Bean fun sqlXmlHandler() = Jdbc4SqlXmlHandler()
    }
}
