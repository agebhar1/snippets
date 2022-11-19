@file:Suppress("CLASS_NAME_INCORRECT", "FILE_NAME_INCORRECT")

package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@AutoConfigureTestDatabase(replace = NONE)
@JdbcTest(properties = ["spring.datasource.url=jdbc:tc:postgresql:14.6:///"])
class PostgreSQLContainerIntTest {
    @Test
    fun `JDBC template is not null`(@Autowired jdbcTemplate: JdbcTemplate) {
        assertThat(jdbcTemplate).isNotNull
    }

    @Test
    fun `testcontainers provides PostgreSQL JDBC URL`(@Autowired dataSource: DataSource) {
        assertThat(dataSource.connection.metaData.url).startsWith("jdbc:postgresql://localhost:")
    }
}
