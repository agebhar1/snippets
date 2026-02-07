package io.github.agebhar1.snippets.quarkus.logging;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.readAllLines;

@QuarkusTest
@TestProfile(StructuredJsonEcsLoggingTest.class)
public class StructuredJsonEcsLoggingTest implements QuarkusTestProfile {

    private static final Path logFilePath;

    static {
        try {
            logFilePath = createTempFile("junit-", "-quarkus.log");
            logFilePath.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.file.enabled", "true",
                "quarkus.log.file.path", logFilePath.toString(),
                "quarkus.log.json.file.enabled", "true",
                "quarkus.log.json.pretty-print", "false",
                "quarkus.log.json.log-format", "ecs",
                "quarkus.log.json.console.enabled", "true"
        );
    }

    @Test
    public void tbd() throws IOException {

        var lines = readAllLines(logFilePath);

        System.out.println(lines);

    }

}
