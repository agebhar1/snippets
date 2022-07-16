package io.github.agebhar1.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.integration.channel.QueueChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.file.FileReadingMessageSource
import org.springframework.integration.file.FileReadingMessageSource.WatchEventType.CREATE
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec
import org.springframework.integration.file.dsl.Files.inboundAdapter
import org.springframework.integration.file.dsl.Files.toStringTransformer
import org.springframework.test.context.junit.jupiter.SpringExtension

import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit

import kotlin.io.path.div
import kotlin.io.path.moveTo
import kotlin.io.path.writeText

@ExtendWith(SpringExtension::class)
class SpringIntegrationFileIntTest {
    @Test
    fun `file content should be read and transformed also file should be kept`(
        @Autowired output: QueueChannel
    ) {
        val file =
            with(tmpPath / "some.txt.write") {
                writeText("content")
                moveTo(tmpPath / "some.txt")
            }

        val message = output.receive()

        assertThat(message).matches { it?.payload is String && it.payload == "CONTENT" }
        assertThat(file).exists()
    }

    @EnableIntegration
    @TestConfiguration
    class Configuration {
        @Bean fun output(): QueueChannel = MessageChannels.queue().get()

        @Bean
        fun flow() =
            integrationFlow(
                filesFromDirectory(tmpPath) {
                    patternFilter("*.txt")
                    watchEvents(CREATE)
                    useWatchService(true)
                },
                { poller { it.fixedRate(1, TimeUnit.SECONDS, 1) } }) {
                transform(toStringTransformer())
                transform<String> { it.uppercase(Locale.getDefault()) }
                channel(output())
            }

        private fun filesFromDirectory(
            directory: Path,
            configurer: FileInboundChannelAdapterSpec.() -> Unit
        ): FileReadingMessageSource = inboundAdapter(directory.toFile()).also { configurer(it) }.get()
    }

    companion object {
        @TempDir @JvmStatic lateinit var tmpPath: Path
    }
}
