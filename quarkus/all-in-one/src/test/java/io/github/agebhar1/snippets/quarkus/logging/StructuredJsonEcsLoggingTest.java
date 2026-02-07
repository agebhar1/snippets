package io.github.agebhar1.snippets.quarkus.logging;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static io.quarkiverse.loggingjson.providers.KeyValueStructuredArgument.kv;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newBufferedReader;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(StructuredJsonEcsLoggingTest.class)
public class StructuredJsonEcsLoggingTest implements QuarkusTestProfile {

    private static final Logger logger = LoggerFactory.getLogger("io.github.agebhar1.snippets.quarkus.logging");

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.file.enabled", "true",
                "quarkus.log.file.path", createTempLogFile().toString(),
                "quarkus.log.json.console.enabled", "true",
                "quarkus.log.json.file.enabled", "true",
                "quarkus.log.json.log-format", "ecs",
                "quarkus.log.json.pretty-print", "false",
                "quarkus.log.json.fields.arguments.field-name", "arguments",
                "quarkus.log.json.additional-field.\"field-name-1\".value", "12345",
                "quarkus.log.json.additional-field.\"field-name-1\".type", "string",
                "quarkus.otel.sdk.disabled", "true"
        );
    }

    @Test
    @DisplayName("should log structured by ECS with given configuration")
    public void shouldLogStructured() throws IOException {
        var config = ConfigProvider.getConfig();
        var file = Path.of(config.getValue("quarkus.log.file.path", String.class));

        try (var br = newBufferedReader(file)) {
            var ignored = br.skip(Files.size(file));

            logger.info("structured log message", kv("1st", "value"), kv("2nd", 0));

            await()
                    .atMost(2, SECONDS)
                    .untilAsserted(() -> {
                        assertThatJson(br.readLine())
                                .isEqualTo("""
                                        {
                                            "@timestamp": "${json-unit.any-string}",
                                            "log.logger": "io.github.agebhar1.snippets.quarkus.logging",
                                            "log.level": "INFO",
                                            "process.thread.name": "${json-unit.any-string}",
                                            "process.thread.id": "${json-unit.any-number}",
                                            "host.name": "${json-unit.any-string}",
                                            "arguments": {
                                                "1st": "value",
                                                "2nd": 0
                                            },
                                            "field-name-1": "12345",
                                            "message": "structured log message",
                                            "ecs.version": "9.0.0"
                                        }
                                        """);

                    });
        }
    }

    static Path createTempLogFile() {
        try {
            var path = createTempFile(Path.of("target"), "junit-", "-quarkus.log");
            path.toFile().deleteOnExit();
            return path;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
