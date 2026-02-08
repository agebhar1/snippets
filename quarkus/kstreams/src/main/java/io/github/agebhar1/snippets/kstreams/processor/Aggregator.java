package io.github.agebhar1.snippets.kstreams.processor;

import io.github.agebhar1.snippets.kstreams.Ping;
import io.github.agebhar1.snippets.kstreams.TopologyProducer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singleton;
import static org.apache.kafka.streams.processor.PunctuationType.WALL_CLOCK_TIME;
import static org.apache.kafka.streams.state.Stores.keyValueStoreBuilder;
import static org.apache.kafka.streams.state.Stores.persistentKeyValueStore;

public class Aggregator extends ContextualProcessor<String, Ping, String, List<Ping>> {

    private static final Logger logger = LoggerFactory.getLogger(Aggregator.class);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm");

    private final boolean punctuationEnabled;
    private final String storeName;
    private KeyValueStore<String, List<Ping>> store;

    public Aggregator(boolean punctuationEnabled, String storeName) {
        this.punctuationEnabled = punctuationEnabled;
        this.storeName = storeName;
    }

    @Override
    public void init(ProcessorContext<String, List<Ping>> context) {
        super.init(context);
        store = context.getStateStore(storeName);
        if (store == null) {
            throw new IllegalStateException(format("Store %s not found", storeName));
        }
        if (punctuationEnabled) {
            context.schedule(Duration.ofSeconds(30), WALL_CLOCK_TIME, this::callback);
        }
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

        var timestamp = Instant.ofEpochMilli(record.timestamp()).atZone(UTC);

        var key = timestamp.format(formatter) + "/" + record.key();
        var value = Optional.ofNullable(store.get(key)).orElse(new ArrayList<>());
        value.add(record.value());

        store.put(key, value);
    }

    static class Supplier implements ProcessorSupplier<String, Ping, String, List<Ping>> {

        private static final String storeName = "ping-store";
        private final boolean punctuationEnabled;

        public Supplier(boolean punctuationEnabled) {
            this.punctuationEnabled = punctuationEnabled;
        }

        @Override
        public Processor<String, Ping, String, List<Ping>> get() {
            return new Aggregator(punctuationEnabled, storeName);
        }

        @Override
        public Set<StoreBuilder<?>> stores() {
            return singleton(
                    keyValueStoreBuilder(
                            persistentKeyValueStore(storeName),
                            Serdes.String(),
                            Serdes.ListSerde(TopologyProducer.PingList.class, new ObjectMapperSerde<>(Ping.class))
                    ));
        }
    }

    public static ProcessorSupplier<String, Ping, String, List<Ping>> supply(boolean punctuationEnabled) {
        return new Supplier(punctuationEnabled);
    }

}
