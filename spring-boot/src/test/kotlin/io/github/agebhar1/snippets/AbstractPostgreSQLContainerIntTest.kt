package io.github.agebhar1.snippets

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

abstract class AbstractPostgreSQLContainerIntTest {

  companion object {

    @Container
    val container =
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:14.0")).apply {
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
