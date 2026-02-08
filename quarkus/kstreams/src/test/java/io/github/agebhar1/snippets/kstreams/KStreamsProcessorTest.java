package io.github.agebhar1.snippets.kstreams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.MockProcessorContext;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.apache.kafka.streams.processor.PunctuationType.STREAM_TIME;
import static org.apache.kafka.streams.processor.PunctuationType.WALL_CLOCK_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// https://kafka.apache.org/42/streams/developer-guide/testing/

public class KStreamsProcessorTest {

    private Processor<String, Integer, String, Integer> processorUnderTest;
    private MockProcessorContext<String, Integer> context;
    private KeyValueStore<String, Integer> store;

    @BeforeEach
    public void setup() {
        processorUnderTest = new CustomMaxAggregator();

        store = Stores
                .keyValueStoreBuilder(
                        Stores.inMemoryKeyValueStore("myStore"),
                        Serdes.String(),
                        Serdes.Integer())
                .withLoggingDisabled() // Changelog is not supported by MockProcessorContext.
                .build();

        context = new MockProcessorContext<>();
        store.init(context.getStateStoreContext(), store);
        context.addStateStore(store);
        processorUnderTest.init(context);

        store.put("a", 21);
    }

    @Test
    public void shouldNotUpdateStoreForSmallerValue() {
        var recordTime = Instant.now();
        processorUnderTest.process(new Record<>("a", 1, recordTime.toEpochMilli()));
        assertEquals(21, store.get("a"));
        assertTrue(context.forwarded().isEmpty());
    }

    @Test
    public void shouldUpdateStoreForLargerValue() {
        var recordTime = Instant.now();
        processorUnderTest.process(new Record<>("a", 42, recordTime.toEpochMilli()));
        assertEquals(42, store.get("a"));
        assertTrue(context.forwarded().isEmpty());
    }

    @Test
    public void shouldUpdateStoreForNewKey() {
        var recordTime = Instant.now();
        processorUnderTest.process(new Record<>("b", 21, recordTime.toEpochMilli()));
        assertEquals(21, store.get("a"));
        assertEquals(21, store.get("b"));
        assertTrue(context.forwarded().isEmpty());
    }

    @Test
    public void shouldForwardIfRunWallClockTimePunctuation() {
        var recordTime = Instant.now();
        processorUnderTest.process(new Record<>("a", 42, recordTime.toEpochMilli()));

        var capturedPunctuator = context.scheduledPunctuators().getFirst();
        assertEquals(WALL_CLOCK_TIME, capturedPunctuator.getType());

        var punctuator = capturedPunctuator.getPunctuator();

        var timestamp = recordTime.plusSeconds(1).toEpochMilli();
        punctuator.punctuate(timestamp);

        assertEquals(new Record<>("a", 42, timestamp), context.forwarded().getFirst().record());
    }

    @Test
    public void shouldForwardIfRunStreamTimePunctuation() {
        var recordTime = Instant.now();
        processorUnderTest.process(new Record<>("a", 42, recordTime.toEpochMilli()));

        var capturedPunctuator = context.scheduledPunctuators().get(1);
        assertEquals(STREAM_TIME, capturedPunctuator.getType());

        var punctuator = capturedPunctuator.getPunctuator();

        var timestamp = recordTime.plusSeconds(1).toEpochMilli();
        punctuator.punctuate(timestamp);

        assertEquals(new Record<>("a", 42, timestamp), context.forwarded().getFirst().record());
    }

    static class CustomMaxAggregator extends ContextualProcessor<String, Integer, String, Integer> {

        private KeyValueStore<String, Integer> store;

        @Override
        public void init(ProcessorContext<String, Integer> context) {
            super.init(context);
            context.schedule(Duration.ofSeconds(60), WALL_CLOCK_TIME, this::flushStore);
            context.schedule(Duration.ofSeconds(10), STREAM_TIME, this::flushStore);
            store = context.getStateStore("myStore");
        }

        @Override
        public void process(Record<String, Integer> record) {
            var oldValue = store.get(record.key());
            if (oldValue == null || record.value() > oldValue) {
                store.put(record.key(), record.value());
            }
        }

        private void flushStore(long timestamp) {
            try (var it = store.all()) {
                while (it.hasNext()) {
                    var next = it.next();
                    context().forward(new Record<>(next.key, next.value, timestamp));
                }
            }
        }

    }

}
