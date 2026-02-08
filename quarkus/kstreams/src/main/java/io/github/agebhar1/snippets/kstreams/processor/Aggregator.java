package io.github.agebhar1.snippets.kstreams.processor;

import io.github.agebhar1.snippets.kstreams.Ping;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.kafka.streams.processor.PunctuationType.WALL_CLOCK_TIME;

public class Aggregator extends ContextualProcessor<String, Ping, String, List<Ping>> {

    private static final Logger logger = LoggerFactory.getLogger(Aggregator.class);

    private final String storeName;
    private KeyValueStore<String, List<Ping>> store;

    public Aggregator(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(ProcessorContext<String, List<Ping>> context) {
        super.init(context);
        store = context.getStateStore(storeName);
        if (store == null) {
            throw new IllegalStateException(format("Store %s not found", storeName));
        }

        context.schedule(Duration.ofSeconds(30), WALL_CLOCK_TIME, this::callback);
    }

    private void callback(long timestamp) {
        logger.info("Periodic operation at: {}", timestamp);
        try (var all = store.all()) {
            all.forEachRemaining(value -> {
                logger.info("[{}] {} -> {}", timestamp, value.key, value.value);

                context().forward(new Record<>(value.key, value.value, timestamp));

                store.delete(value.key);
            });
        }
    }

    @Override
    public void process(Record<String, Ping> record) {
        logger.info("aggregate [{}] {} -> {}", record.timestamp(), record.key(), record.value());

        if (record.key().equals("5")) {
            context().forward(new Record<>(record.key(), List.of(record.value()), Instant.now().toEpochMilli()));
        } else {
            var value = Optional.ofNullable(store.get(record.key())).orElse(new ArrayList<>());
            value.add(record.value());
            store.put(record.key(), value);
        }
    }

}
