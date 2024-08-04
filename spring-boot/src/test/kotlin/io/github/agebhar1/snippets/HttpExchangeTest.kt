package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.annotation.Retryable
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.wiremock.integrations.testcontainers.WireMockContainer

data class ToDo(
    val id: String,
    val title: String,
    val completed: Boolean
)

@HttpExchange(
    url = "/todos",
    accept = ["application/json"],
    contentType = "application/json"
)
@Retryable(backoff = Backoff(delay = 100, multiplier = 1.5, random = true))
interface ToDosService {
    @GetExchange
    fun getAll(): List<ToDo>

    @GetExchange("/{id}")
    fun getById(@PathVariable id: String): ToDo?

    @PostExchange
    fun save(@RequestBody todo: ToDo): ToDo

    @DeleteExchange("/{id}")
    fun delete(@PathVariable id: String)
}

@ExtendWith(SpringExtension::class)
@Testcontainers
class HttpExchangeTest(@Autowired private val client: ToDosService) {
    @Test
    fun `(service) client should get all ToDo items`() {
        assertThat(client.getAll())
            .containsExactly(
                ToDo(id = "199d25ae-42e2-4cc8-9a97-571a46e25689", title = "Item #1", completed = true),
                ToDo(id = "d2db635a-27e4-4a63-9d07-1a19aa52ace8", title = "Item #2", completed = false),
                ToDo(id = "dc800874-bad2-4aeb-ada6-ca8f7630c6a2", title = "Item #3", completed = false)
            )
    }

    @Test
    fun `(service) client should get ToDo item by Id`() {
        assertThat(client.getById("199d25ae-42e2-4cc8-9a97-571a46e25689"))
            .isEqualTo(
                ToDo(id = "199d25ae-42e2-4cc8-9a97-571a46e25689", title = "Item #1", completed = true)
            )
    }

    @Test
    fun `(service) client should save ToDo item`() {
        val todo = ToDo(id = "34c93fba-da84-45f6-921a-9ab6322d9ee0", title = "Item #4", completed = false)
        assertThat(client.save(todo)).isEqualTo(todo)
    }

    @Test
    fun `(service) client should delete ToDo item by Id`() {
        client.delete("199d25ae-42e2-4cc8-9a97-571a46e25689")
    }

    @TestConfiguration
    @EnableRetry
    class Configuration {
        @Bean
        fun client(): ToDosService {
            val webClient = WebClient.builder().baseUrl(wm.baseUrl).build()
            val factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build()

            return factory.createClient(ToDosService::class.java)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HttpExchangeTest::class.java)

        @Container
        val wm: WireMockContainer = WireMockContainer("wiremock/wiremock:3.9.1-1")
            .withLogConsumer(Slf4jLogConsumer(logger).withSeparateOutputStreams())
            .withMappingFromResource("todo", "wiremock/todo-stubs.json")
    }
}
