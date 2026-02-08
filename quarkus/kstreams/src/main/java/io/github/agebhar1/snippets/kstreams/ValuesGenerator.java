package io.github.agebhar1.snippets.kstreams;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@ApplicationScoped
public class ValuesGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ValuesGenerator.class);

    private final boolean enabled;

    public ValuesGenerator(@ConfigProperty(name = "app.generator.enabled") boolean enabled) {
        this.enabled = enabled;
    }

    @Outgoing("ping")
    public Multi<Record<String, Ping>> ping() {
        if (enabled) {
            return Multi.createFrom().ticks().every(Duration.ofMillis(2500))
                    .onOverflow().drop()
                    .map(tick -> {
                        var ping = new Ping(String.format("tick: %d", tick));
                        logger.info("{}", ping);
                        return Record.of(String.valueOf(tick % 10), ping);
                    });
        } else {
            return Multi.createFrom().items();
        }
    }

}
