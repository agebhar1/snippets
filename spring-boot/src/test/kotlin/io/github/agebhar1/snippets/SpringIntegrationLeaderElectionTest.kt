@file:Suppress("LONG_LINE")

package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.integration.jdbc.lock.DefaultLockRepository
import org.springframework.integration.jdbc.lock.JdbcLockRegistry
import org.springframework.integration.jdbc.lock.LockRepository
import org.springframework.integration.leader.Candidate
import org.springframework.integration.leader.Context
import org.springframework.integration.leader.event.LeaderEventPublisher
import org.springframework.integration.leader.event.OnGrantedEvent
import org.springframework.integration.leader.event.OnRevokedEvent
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator
import org.springframework.integration.support.locks.LockRegistry
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@JdbcTest(properties = ["spring.datasource.url=jdbc:tc:postgresql:15.2:///"])
class SpringIntegrationLeaderElectionTest(
    @Autowired val leaderInitiator: LockRegistryLeaderInitiator,
    @Autowired val lockRegistry: LockRegistry,
    @Autowired val singleResource: SingleResource
) {
    /*
        http://presos.dsyer.com/decks/locks-and-leaders.html
        https://docs.spring.io/spring-integration/docs/current/reference/html/jdbc.html#jdbc-lock-registry
        https://github.com/spring-projects/spring-integration/blob/main/spring-integration-jdbc/src/test/java/org/springframework/integration/jdbc/leader/JdbcLockRegistryLeaderInitiatorTests.java
     */

    @AfterEach
    fun reset() {
        leaderInitiator.stop()
        leaderInitiator.setLeaderEventPublisher(null)
    }

    @Test
    fun `bean should receive leadership events by default via @EventListener for 'OnGrantedEvent' and 'OnRevokedEvent'`() {
        leaderInitiator.start()

        assertThat(singleResource.granted.await(2, SECONDS)).isTrue
        assertThat(leaderInitiator.context.isLeader).isTrue

        leaderInitiator.stop()

        assertThat(singleResource.revoked.await(2, SECONDS)).isTrue
        assertThat(leaderInitiator.context.isLeader).isFalse
    }

    @Test
    fun `dedicated 'LeaderEventPublisher' should be invoked by leadership events`() {
        val granted = CountDownLatch(1)
        val revoked = CountDownLatch(1)

        leaderInitiator.setLeaderEventPublisher(object : LeaderEventPublisher {
            override fun publishOnGranted(
                source: Any?,
                context: Context?,
                role: String?
            ) {
                granted.countDown()
            }

            override fun publishOnRevoked(
                source: Any?,
                context: Context?,
                role: String?
            ) {
                revoked.countDown()
            }

            override fun publishOnFailedToAcquire(
                source: Any?,
                context: Context?,
                role: String?
            ) {
            }
        })

        leaderInitiator.start()

        assertThat(granted.await(2, SECONDS)).isTrue
        assertThat(leaderInitiator.context.isLeader).isTrue

        leaderInitiator.stop()

        assertThat(revoked.await(2, SECONDS)).isTrue
        assertThat(leaderInitiator.context.isLeader).isFalse
    }

    @Test
    fun `dedicated 'Candidate' should be invoked by leadership events`() {
        val candidate = AnyCandidate()
        val leaderInitiator = LockRegistryLeaderInitiator(lockRegistry, candidate)

        leaderInitiator.start()

        assertThat(candidate.granted.await(2, SECONDS)).isTrue
        assertThat(leaderInitiator.context.isLeader).isTrue

        leaderInitiator.stop()

        assertThat(candidate.revoked.await(2, SECONDS)).isTrue
        assertThat(leaderInitiator.context.isLeader).isFalse
    }

    class SingleResource {
        var granted = CountDownLatch(1)
        var revoked = CountDownLatch(1)

        @EventListener(OnGrantedEvent::class)
        fun start() {
            granted.countDown()
        }

        @EventListener(OnRevokedEvent::class)
        fun stop() {
            revoked.countDown()
        }
    }

    class AnyCandidate : Candidate {
        var granted = CountDownLatch(1)
        var revoked = CountDownLatch(1)
        private val id = UUID.randomUUID().toString()

        override fun getRole() = "leader"

        override fun getId() = id

        override fun onGranted(ctx: Context) {
            granted.countDown()
        }

        override fun onRevoked(ctx: Context) {
            revoked.countDown()
        }
    }

    @TestConfiguration
    class Configuration {
        @Bean
        fun lockRepository(dataSource: DataSource) = DefaultLockRepository(dataSource, "IdOne").apply {
            setRegion("RegionOne")
        }

        @Bean
        fun lockRegistry(client: LockRepository) = JdbcLockRegistry(client)

        @Bean
        fun leaderInitiator(lockRegistry: LockRegistry) = LockRegistryLeaderInitiator(lockRegistry).apply {
            setHeartBeatMillis(1000)
        }

        @Bean
        fun singleResource() = SingleResource()
    }
}
