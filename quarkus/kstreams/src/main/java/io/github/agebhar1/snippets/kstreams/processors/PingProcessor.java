package io.github.agebhar1.snippets.kstreams.processors;

import io.github.agebhar1.snippets.kstreams.entity.Ping;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PingProcessor extends ContextualProcessor<String, Ping, Void, Void> {

    private static final Logger logger = LoggerFactory.getLogger(PingProcessor.class);
    private final String storeName;
    private KeyValueStore<String, List<Ping>> store;

    public PingProcessor(final String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(final ProcessorContext<Void, Void> context) {
        super.init(context);
        store = context.getStateStore(storeName);
//        context.schedule(Duration.ofSeconds(5), WALL_CLOCK_TIME, (timestamp) -> {
//            logger.info("Periodic operation at: {}", timestamp);
//        });
    }

    @Override
    public void process(final Record<String, Ping> record) {
        var key = record.key();
        var value = record.value();
        var previous = Optional.ofNullable(store.get(key)).orElseGet(ArrayList::new);

        logger.info("Process: {} => {} (prev: {})", key, value, previous);

        previous.add(value);
        store.put(key, previous);
    }

}