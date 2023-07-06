@file:Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")

package io.github.agebhar1.snippets

import io.github.agebhar1.snippets.SpringIntegrationInboundJmsEventualConsistencyTest.InboxXmlMessageRepositoryInterceptorAdvice
import io.github.agebhar1.snippets.SpringIntegrationInboundJmsEventualConsistencyTest.TransactionTemplateInterceptorAdvice
import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.domain.InboxXmlMessageRepository
import io.github.agebhar1.snippets.repository.JdbcInboxXmlMessageRepository
import jakarta.jms.ConnectionFactory
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.endpoint.AbstractEndpoint
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.listener.DefaultMessageListenerContainer
import org.springframework.jms.support.destination.DynamicDestinationResolver
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestConstructor.AutowireMode.ALL
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.ErrorHandler
import org.springframework.util.IdGenerator
import java.io.IOException
import java.sql.SQLException
import java.time.Clock
import java.time.Clock.fixed
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS

@Service
class CustomJmsConsumer(
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val repository: InboxXmlMessageRepository,
    private val connectionFactory: ConnectionFactory,
    private val txTemplate: TransactionTemplate,
) : AbstractEndpoint() {
    private val messageListenerContainer = kotlin.run {
        val container = DefaultMessageListenerContainer().apply {
            destinationName = "inbound.queue"
            destinationResolver = DynamicDestinationResolver()
            errorHandler = ErrorHandler { throw it }
            isSessionTransacted = true
            messageListener = MessageListener { onMessage(it) }
        }
        @Suppress("COMPACT_OBJECT_INITIALIZATION")
        container.connectionFactory = connectionFactory

        container.apply { initialize() }
    }

    private fun onMessage(message: Message) =
        txTemplate.execute {
            val document = (message as TextMessage).text.toDocument()

            repository.save(
                InboxXmlMessage(
                    id = idGenerator.generateId(),
                    occurredAtUtc = Instant.now(clock),
                    type = "a",
                    data = document))

            repository.save(
                InboxXmlMessage(
                    id = idGenerator.generateId(),
                    occurredAtUtc = Instant.now(clock),
                    type = "b",
                    data = document))
        }

    override fun destroy() = messageListenerContainer.shutdown()

    override fun onInit() = messageListenerContainer.initialize()

    override fun doStart() = messageListenerContainer.start()

    override fun doStop() = messageListenerContainer.stop()

    override fun setAutoStartup(autoStartup: Boolean) {
        super.setAutoStartup(autoStartup)
        messageListenerContainer.isAutoStartup = autoStartup
    }
}

@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@Import(
    value =
        [
            InboxXmlMessageRepositoryInterceptorAdvice::class,
            JdbcInboxXmlMessageRepository::class,
            TransactionTemplateInterceptorAdvice::class,
        ])
@SpringBootTest(properties = ["spring.artemis.embedded.queues=inbound.queue"])
@Sql(
    executionPhase = AFTER_TEST_METHOD,
    statements =
        ["DELETE FROM INBOX_XML_MESSAGE WHERE id = '4bafe8fd-2086-4abb-a79f-47bbaa0aa4c9'"])
@TestConstructor(autowireMode = ALL)
@Transactional(propagation = NEVER)
class SpringIntegrationInboundJmsEventualConsistencyTest(
    private val consumer: CustomJmsConsumer,
    private val repositoryInterceptor: InboxXmlMessageRepositoryInterceptor,
    private val jdbcTemplate: JdbcTemplate,
    private val jmsTemplate: JmsTemplate,
    private val txTemplateInterceptor: TransactionTemplateInterceptor
) {
    @BeforeEach
    fun beforeEach() {
        consumer.start()
        repositoryInterceptor.reset()
        with(txTemplateInterceptor) {
            beforeExecute = { consumer.stop() }
            afterExecute = {}
        }
    }

    @Test
    fun `should not contain database items and keep JMS message if an exception occurs while save to repository`() {
        val latch = CountDownLatch(1)

        with(repositoryInterceptor) {
            beforeSaveExecution = {
                latch.countDown()
                throw SQLException("Something went wrong while save to repository")
            }
        }

        jmsTemplate.send("inbound.queue") { session -> session.createTextMessage("<some>data</some>") }

        assertThat(latch.await(1000, MILLISECONDS)).isTrue

        val actual =
            jdbcTemplate.queryForObject(
                "SELECT DISTINCT COUNT(*) FROM INBOX_XML_MESSAGE", Long::class.java)
        assertThat(actual).isEqualTo(0)

        jmsTemplate.receiveTimeout = 250
        val message = jmsTemplate.receive("inbound.queue")
        assertThat(message).isNotNull
    }

    @Test
    fun `should not contain first database item (a) and keep JMS message if an exception occurs after saved to repository`() {
        val latch = CountDownLatch(1)

        with(repositoryInterceptor) {
            afterSaveExecution = {
                latch.countDown()
                /* a checked exception to NOT rollback transaction */
                throw IOException("Something went wrong after saved to repository")
            }
        }

        jmsTemplate.send("inbound.queue") { session -> session.createTextMessage("<some>data</some>") }

        assertThat(latch.await(1000, MILLISECONDS)).isTrue

        val actual = jdbcTemplate.queryForList("SELECT type FROM INBOX_XML_MESSAGE", String::class.java)
        assertThat(actual).isEmpty()

        jmsTemplate.receiveTimeout = 250
        val message = jmsTemplate.receive("inbound.queue")
        assertThat(message).isNotNull
    }

    @Test
    fun `should not contain second database item (b) and keep JMS message if an exception occurs after saved to repository`() {
        val latch = CountDownLatch(1)

        with(repositoryInterceptor) {
            afterSaveExecution = { message ->
                if (message.type == "b") {
                    latch.countDown()
                    /* a checked exception to NOT rollback transaction */
                    throw IOException("Something went wrong after saved to repository")
                }
            }
        }

        jmsTemplate.send("inbound.queue") { session -> session.createTextMessage("<some>data</some>") }

        assertThat(latch.await(1000, MILLISECONDS)).isTrue

        val actual = jdbcTemplate.queryForList("SELECT type FROM INBOX_XML_MESSAGE", String::class.java)
        assertThat(actual).isEmpty()

        jmsTemplate.receiveTimeout = 250
        val message = jmsTemplate.receive("inbound.queue")
        assertThat(message).isNotNull
    }

    @Test
    fun `should contain database items and consume JMS message if no exception occurs`() {
        val latch = CountDownLatch(1)

        with(txTemplateInterceptor) { afterExecute = { latch.countDown() } }

        jmsTemplate.send("inbound.queue") { session -> session.createTextMessage("<some>data</some>") }

        assertThat(latch.await(1000, MILLISECONDS)).isTrue

        val actual = jdbcTemplate.queryForList("SELECT type FROM INBOX_XML_MESSAGE", String::class.java)
        assertThat(actual).containsExactlyInAnyOrder("a", "b")

        jmsTemplate.receiveTimeout = 250
        val message = jmsTemplate.receive("inbound.queue")
        assertThat(message).isNull()
    }

    @Test
    fun `should contain database items and keep JMS message if an exception occurs after saved to repository`() {
        val latch = CountDownLatch(1)

        with(txTemplateInterceptor) {
            afterExecute = {
                latch.countDown()
                throw java.lang.RuntimeException("Something went wrong after saved to repository")
            }
        }

        jmsTemplate.send("inbound.queue") { session -> session.createTextMessage("<some>data</some>") }

        assertThat(latch.await(1000, MILLISECONDS)).isTrue

        val actual = jdbcTemplate.queryForList("SELECT type FROM INBOX_XML_MESSAGE", String::class.java)
        assertThat(actual).containsExactlyInAnyOrder("a", "b")

        jmsTemplate.receiveTimeout = 250
        val message = jmsTemplate.receive("inbound.queue")
        assertThat(message).isNotNull
    }

    data class InboxXmlMessageRepositoryInterceptor(
        var beforeSaveExecution: ((InboxXmlMessage) -> Unit) = {},
        var afterSaveExecution: ((InboxXmlMessage) -> Unit) = {}
    ) {
        fun reset() {
            beforeSaveExecution = {}
            afterSaveExecution = {}
        }
    }

    @Aspect
    @Component
    class InboxXmlMessageRepositoryInterceptorAdvice(
        private val interceptor: InboxXmlMessageRepositoryInterceptor
    ) {
        @Pointcut(
            "execution(void *.InboxXmlMessageRepository.save(*.InboxXmlMessage)) && args(message)")
        fun saveExecution(message: InboxXmlMessage) {}

        @Around("saveExecution(message)")
        fun aroundAdvice(proceedingJoinPoint: ProceedingJoinPoint, message: InboxXmlMessage): Any? {
            interceptor.beforeSaveExecution.invoke(message)
            val returnValue: Any? = proceedingJoinPoint.proceed()
            interceptor.afterSaveExecution.invoke(message)

            return returnValue
        }
    }

    data class TransactionTemplateInterceptor(
        var beforeExecute: (() -> Unit) = {},
        var afterExecute: (() -> Unit) = {}
    )

    @Aspect
    @Component
    class TransactionTemplateInterceptorAdvice(
        private val interceptor: TransactionTemplateInterceptor
    ) {
        @Pointcut(
            "execution(public * org.springframework..TransactionTemplate.execute(..) throws org.springframework..TransactionException)")
        fun executeExecution() {}

        @Around("executeExecution()")
        fun aroundAdvice(proceedingJoinPoint: ProceedingJoinPoint): Any? {
            interceptor.beforeExecute.invoke()
            val returnValue: Any? = proceedingJoinPoint.proceed()
            interceptor.afterExecute.invoke()

            return returnValue
        }
    }

    @EnableIntegration
    @TestConfiguration
    class Configuration {
        @Bean fun repositoryInterceptor() = InboxXmlMessageRepositoryInterceptor()

        @Bean fun txTemplateInterceptor() = TransactionTemplateInterceptor()

        @Bean fun clock(): Clock = fixed(Instant.parse("2022-05-07T14:05:00Z"), UTC)

        @Bean
        fun idGenerator() = IdGenerator { UUID.fromString("4bafe8fd-2086-4abb-a79f-47bbaa0aa4c9") }

        @Bean fun sqlXmlHandler() = Jdbc4SqlXmlHandler()
    }
}
