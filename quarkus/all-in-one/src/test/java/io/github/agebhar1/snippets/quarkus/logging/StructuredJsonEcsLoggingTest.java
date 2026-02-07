package io.github.agebhar1.snippets.quarkus.logging;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.file.Files.readAllLines;

@QuarkusTest
@TestProfile(StructuredJsonEcsLoggingTest.class)
public class StructuredJsonEcsLoggingTest implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.file.enabled", "true",
                "quarkus.log.file.path", createTempLogFile().toString(),
                "quarkus.log.json.console.enabled", "true",
                "quarkus.log.json.file.enabled", "true",
                "quarkus.log.json.log-format", "ecs",
                "quarkus.log.json.pretty-print", "false",
                "quarkus.otel.sdk.disabled", "true"
        );
    }

    @Test
    public void tbd() throws IOException {
        var config = ConfigProvider.getConfig();
        var file = config.getValue("quarkus.log.file.path", String.class);

        var lines = readAllLines(Path.of(file));
        System.out.println(lines);
    }

    static Path createTempLogFile() {
        try {
            var path = Files.createTempFile(Path.of("target"),"junit-", "-quarkus.log");
            path.toFile().deleteOnExit();
            return path;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
