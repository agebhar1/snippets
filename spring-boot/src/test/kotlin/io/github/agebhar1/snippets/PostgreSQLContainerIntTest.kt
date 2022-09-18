@file:Suppress("CLASS_NAME_INCORRECT", "FILE_NAME_INCORRECT")

package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@JdbcTest
@Testcontainers
class PostgreSQLContainerIntTest {
    @Test
    fun `JDBC template is not null`(@Autowired jdbcTemplate: JdbcTemplate) {
        assertThat(jdbcTemplate).isNotNull
    }

    @Test
    fun `container provides PostgreSQL JDBC URL`() {
        assertThat(container.jdbcUrl).startsWith("jdbc:postgresql://localhost:")
    }

    companion object {
        @Container
        val container =
            PostgreSQLContainer(DockerImageName.parse("postgres:14.5")).apply {
                withDatabaseName("postgres")
                withUsername("postgres")
                withPassword("postgres")
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", container::getJdbcUrl)
            registry.add("spring.datasource.password", container::getPassword)
            registry.add("spring.datasource.username", container::getUsername)
        }
    }
}
