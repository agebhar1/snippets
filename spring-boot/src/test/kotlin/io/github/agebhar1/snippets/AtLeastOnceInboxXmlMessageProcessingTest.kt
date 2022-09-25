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
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.postgresql.jdbc.PgSQLXML
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.xml.Jdbc4SqlXmlHandler
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
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
import javax.transaction.Transactional
import javax.transaction.Transactional.TxType.NEVER
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AutoConfigureTestDatabase(replace = NONE)
@Import(
    JdbcInboxXmlMessageRepository::class,
    Jdbc4SqlXmlHandler::class,
    ModifiableFixedUTCClock::class
)
@JdbcTest(properties = ["spring.datasource.url=jdbc:tc:postgresql:14.5:///"])
class AtLeastOnceInboxXmlMessageProcessingTest(
    @Autowired private val clock: ModifiableFixedUTCClock,
    @Autowired private val jdbcTemplate: NamedParameterJdbcTemplate,
    @Autowired private val repository: JdbcInboxXmlMessageRepository
) {
    @ParameterizedTest
    @ValueSource(ints = [1, 5, 10])
    fun `should grab #limit messages if never processed before`(value: Int) {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = value, processorId = "worker-0")).hasSize(value)
        assertThat(count(where = "processing_started_by = 'worker-0'")).isEqualTo(value)
        assertThat(count(where = "processing_started_by IS NULL")).isEqualTo(10 - value)
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
        assertThat(count(where = "processing_started_by IS NULL")).isZero
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
        assertThat(count(where = "processing_started_by IS NULL")).isEqualTo(5)
    }

    @ParameterizedTest
    @ValueSource(ints = [5, 10, 15])
    fun `should 'findAndTagUnprocessedOrExpiredMessages' as much as possible messages if less than #limit are available`(value: Int) {
        populateInboxXmlMessage(1)
        assertThat(count()).isEqualTo(1)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = value, processorId = "worker-0")).hasSize(1)
        assertThat(count(where = "processing_started_by = 'worker-0'")).isOne
        assertThat(count(where = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `invoke 'findAndTagUnprocessedOrExpiredMessages' multiple times with the same processorId succeeds`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        clock += 1.seconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        
        assertThat(count(where = "processing_started_by = 'worker-0'")).isEqualTo(10)
        assertThat(count(where = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `invoke 'findAndTagUnprocessedOrExpiredMessages' multiple times with different processorId succeeds`() {
        populateInboxXmlMessage(10)
        assertThat(count()).isEqualTo(10)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0")).hasSize(5)
        clock += 1.seconds
        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-1")).hasSize(5)

        assertThat(count(where = "processing_started_by = 'worker-0'")).isEqualTo(5)
        assertThat(count(where = "processing_started_by = 'worker-1'")).isEqualTo(5)
        assertThat(count(where = "processing_started_by IS NULL")).isZero
    }

    @Test
    fun `'findAndTagUnprocessedOrExpiredMessages' should return columns 'id', 'type' and 'data'`() {
        populateInboxXmlMessage(1)
        assertThat(count()).isEqualTo(1)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0"))
            .hasSize(1)
            .map(Function { it.keys })
            .containsExactly(setOf("id", "type", "data"))
    }

    @Test
    fun `'findAndTagUnprocessedOrExpiredMessages' should return value types 'UUID', 'String' and 'PqSQLXML'`() {
        populateInboxXmlMessage(1)
        assertThat(count()).isEqualTo(1)

        assertThat(findAndTagUnprocessedOrExpiredMessages(limit = 5, processorId = "worker-0"))
            .hasSize(1)
            .map(Function { it.values.map(Any::javaClass) })
            .containsExactly(listOf(UUID::class.java, String::class.java, PgSQLXML::class.java) as List<Class<Any>>)
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
    @Transactional(NEVER)
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

    private fun findAndTagUnprocessedOrExpiredMessages(
        limit: Int,
        processorId: String,
        ttl: kotlin.time.Duration = 1.seconds
    ): List<Map<String, Any>> {
        val keys = GeneratedKeyHolder()
        val now = LocalDateTime.ofInstant(Instant.now(clock), UTC)
        // https://dba.stackexchange.com/a/69497
        jdbcTemplate
            .update(
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
                            ORDER BY processing_started_at_utc
                            LIMIT :limit
                            FOR UPDATE SKIP LOCKED
                        )
                    RETURNING id, type, data
                """.trimIndent(),
                MapSqlParameterSource(
                    mapOf(
                        "processingStartedAtUTC" to now,
                        "processingStartedBy" to processorId,
                        "expiresAfter" to now.minusNanos(ttl.inWholeNanoseconds),
                        "limit" to limit
                    )),
                keys
            )
        return keys.keyList
    }

    private fun deleteById(id: UUID) = repository.deleteById(id)

    private fun populateInboxXmlMessage(count: Int) {
        val now = Instant.now(clock)
        repeat(count) {
            val entity = InboxXmlMessage(
                id = UUID.randomUUID(),
                occurredAtUtc = now,
                type = "NONE",
                data = "<document/>".toDocument()
            )
            repository.save(entity)
        }
    }

    private fun count(where: String? = null): Int =
        jdbcTemplate
            .queryForObject(
                "SELECT Count(*) FROM INBOX_XML_MESSAGE ${(where?.let { "WHERE $where" } ?: "")}",
                MapSqlParameterSource(),
                Int::class.java) ?: -1

    class ModifiableFixedUTCClock(private var value: Instant = Instant.now()) : Clock() {
        override fun getZone(): ZoneOffset = UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = value

        operator fun plusAssign(duration: kotlin.time.Duration) {
            value = value.plusMillis(duration.inWholeMilliseconds)
        }
    }
}
