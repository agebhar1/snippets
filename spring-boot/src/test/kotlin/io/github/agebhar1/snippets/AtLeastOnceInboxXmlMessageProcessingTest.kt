@file:Suppress(
    "CLASS_NAME_INCORRECT",
    "FILE_NAME_INCORRECT",
    "BACKTICKS_PROHIBITED"
)

package io.github.agebhar1.snippets

import io.github.agebhar1.snippets.AtLeastOnceInboxXmlMessageProcessingTest.ModifiableFixedUTCClock
import io.github.agebhar1.snippets.domain.InboxXmlMessage
import io.github.agebhar1.snippets.repository.JdbcInboxXmlMessageRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.postgresql.jdbc.PgSQLXML
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestConstructor.AutowireMode.ALL
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.function.Function
import kotlin.random.Random.Default.nextLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Import(
    JdbcInboxXmlMessageRepository::class,
    Jdbc4SqlXmlHandler::class,
    ModifiableFixedUTCClock::class
)
@JdbcTest
@TestConstructor(autowireMode = ALL)
class AtLeastOnceInboxXmlMessageProcessingTest(
    private val clock: ModifiableFixedUTCClock,
    private val jdbcClient: JdbcClient,
    private val repository: JdbcInboxXmlMessageRepository
) {
    @ParameterizedTest
    @ValueSource(ints = [1, 5, 10])
    fun `should grab #limit messages if never processed before`(value: Int) {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = value, processorId = "worker-0")).hasSize(value)
        assertThat(count(whereClause = "processing_started_by = 'worker-0'")).isEqualTo(value)
        assertThat(count(whereClause = "processing_started_by IS NULL")).isEqualTo(10 - value)
    }

    @Test
    fun `should grab messages if TTL expired`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        clock += 25.milliseconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-1")).hasSize(5)
        clock += 25.milliseconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-2")).hasSize(0)

        clock += 5.seconds

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-3")).hasSize(5)
        assertThat(count(whereClause = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `'findAndTagUnprocessedOrExpiredMessages' should select messages sorted by there occurrence in ascending order`() {
        populateInboxXmlMessage(10) { Instant.ofEpochSecond(nextLong(from = 0, until = 1_665_254_241)) }

        val actual: MutableList<Timestamp> = mutableListOf()
        repeat(10) {
            actual.addAll(
                findAndTagUnprocessedOrExpiredMessages(limit = 1, processorId = "worker-0").map {
                    it["occurred_at_utc"] as Timestamp
                })
        }
        assertThat(actual).isSorted
    }

    @RepeatedTest(5)
    fun `'findAndTagUnprocessedOrExpiredMessages' should prefer older messages if both expired an new ones exists`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        clock += 25.milliseconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-1")).hasSize(5)
        clock += 25.milliseconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-2")).hasSize(0)

        clock += 5.seconds

        populateInboxXmlMessage(5)
        assertThat(count()).isEqualTo(15)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 10, processorId = "worker-3")).hasSize(10)
        assertThat(count(whereClause = "processing_started_by IS NULL")).isEqualTo(5)
    }

    @ParameterizedTest
    @ValueSource(ints = [5, 10, 15])
    fun `should 'findAndTagUnprocessedOrExpiredMessages' as much as possible messages if less than #limit are available`(value: Int) {
        populateInboxXmlMessage(1)
        assertThat(count()).isEqualTo(1)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = value, processorId = "worker-0")).hasSize(1)
        assertThat(count(whereClause = "processing_started_by = 'worker-0'")).isOne
        assertThat(count(whereClause = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `invoke 'findAndTagUnprocessedOrExpiredMessages' multiple times with the same processorId succeeds`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        clock += 1.seconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        
        assertThat(count(whereClause = "processing_started_by = 'worker-0'")).isEqualTo(10)
        assertThat(count(whereClause = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `invoke 'findAndTagUnprocessedOrExpiredMessages' multiple times with different processorId succeeds`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        clock += 1.seconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-1")).hasSize(5)

        assertThat(count(whereClause = "processing_started_by = 'worker-0'")).isEqualTo(5)
        assertThat(count(whereClause = "processing_started_by = 'worker-1'")).isEqualTo(5)
        assertThat(count(whereClause = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `'findAndTagUnprocessedOrExpiredMessages' should return columns 'id', 'type' and 'data'`() {
        populateInboxXmlMessage(1)
        assertThat(count()).isEqualTo(1)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0"))
            .hasSize(1)
            .map(Function { it.keys })
            .containsExactly(setOf("id", "occurred_at_utc", "type", "data"))
    }

    @Test
    fun `'findAndTagUnprocessedOrExpiredMessages' should return value types 'UUID', 'Timestamp', 'String' and 'PqSQLXML'`() {
        populateInboxXmlMessage(1)
        assertThat(count()).isEqualTo(1)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0"))
            .hasSize(1)
            .map(Function { it.values.map(Any::javaClass) })
            .containsExactly(listOf(UUID::class.java, Timestamp::class.java, String::class.java, PgSQLXML::class.java) as List<Class<Any>>)
    }

    @Test
    fun `once the grabbed message is processed it should be deleted successfully`() {
        populateInboxXmlMessage(1)
        assertThat(count()).isOne

        val keys = findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")
        keys
            .map { it["id"] as UUID }
            .forEach { deleteById(it) }

        assertThat(count()).isZero
    }

    @RepeatedTest(5)
    @Sql(executionPhase = AFTER_TEST_METHOD, statements = ["DELETE FROM INBOX_XML_MESSAGE"])
    @Transactional(propagation = NEVER)
    fun `should run with multiple threads`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        val actual = with(ForkJoinPool()) {
            val futures = invokeAll(List(100) {
                Callable { findAndTagUnprocessedOrExpiredMessages(limit = 2, processorId = "worker-$it") }
            })
            shutdown()
            futures.flatMap { it.get().map { keys -> keys["id"] as UUID } }
        }
        assertThat(actual).hasSize(10)
    }

    @BeforeEach
    fun resetClock() {
        clock.reset()
    }

    private fun findAndTagUnprocessedOrExpiredMessages(
        limit: Int,
        processorId: String,
        ttl: kotlin.time.Duration = 1.seconds
    ): List<Map<String, Any>> {
        val keys = GeneratedKeyHolder()
        val now = LocalDateTime.ofInstant(Instant.now(clock), UTC)
        // https://dba.stackexchange.com/a/69497
        jdbcClient.sql(
            """
                UPDATE INBOX_XML_MESSAGE
                SET processing_started_at_utc = :processingStartedAtUTC,
                    processing_started_by = :processingStartedBy
                WHERE
                    id IN (
                        SELECT id
                        FROM INBOX_XML_MESSAGE
                        WHERE
                            processing_started_at_utc IS NULL OR
                            processing_started_at_utc < :expiresAfter
                        ORDER BY occurred_at_utc
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                    )
                RETURNING id, occurred_at_utc, type, data
            """.trimIndent())
            .param("processingStartedAtUTC", now)
            .param("processingStartedBy", processorId)
            .param("expiresAfter", now.minusNanos(ttl.inWholeNanoseconds))
            .param("limit", limit)
            .update(keys)

        return keys.keyList
    }

    private fun deleteById(id: UUID) = repository.deleteById(id)

    private fun populateInboxXmlMessage(count: Int, now: () -> Instant = { Instant.now(clock) }) {
        repeat(count) {
            val entity = InboxXmlMessage(
                id = UUID.randomUUID(),
                occurredAtUtc = now(),
                type = "NONE",
                data = "<document/>".toDocument()
            )
            repository.save(entity)
        }
    }

    private fun count(whereClause: String? = null): Int =
        whereClause?.let {
            JdbcTestUtils.countRowsInTableWhere(jdbcClient, "INBOX_XML_MESSAGE", whereClause)
        } ?: JdbcTestUtils.countRowsInTable(jdbcClient, "INBOX_XML_MESSAGE")

    class ModifiableFixedUTCClock(private var value: Instant = Instant.now()) : Clock() {
        override fun getZone(): ZoneOffset = UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = value

        operator fun plusAssign(duration: kotlin.time.Duration) {
            value = value.plusMillis(duration.inWholeMilliseconds)
        }

        fun reset() {
            value = Instant.now()
        }
    }
}
