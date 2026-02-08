package io.github.agebhar1.snippets.kstreams;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static org.apache.kafka.streams.processor.PunctuationType.STREAM_TIME;
import static org.apache.kafka.streams.processor.PunctuationType.WALL_CLOCK_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// https://kafka.apache.org/42/streams/developer-guide/testing/

class KStreamsProcessorAPITopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, Long> inputTopic;
    private TestOutputTopic<String, Long> outputTopic;
    private KeyValueStore<String, Long> store;

    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<Long> longSerde = Serdes.Long();

    @BeforeEach
    public void setup() {

        var topology = new Topology()
                .addSource("sourceProcessor", "input-topic")
                .addProcessor("aggregator", new CustomMaxAggregatorSupplier(), "sourceProcessor")
                .addStateStore(
                        Stores.keyValueStoreBuilder(
                                Stores.persistentKeyValueStore("aggStore"),
                                stringSerde,
                                longSerde
                        ),
                        "aggregator")
                .addSink("sinkProcessor", "result-topic", "aggregator");

        // setup test driver
        var props = new Properties();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerde.getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, longSerde.getClass().getName());
        testDriver = new TopologyTestDriver(topology, props);

        // setup test topics
        inputTopic = testDriver.createInputTopic("input-topic", stringSerde.serializer(), longSerde.serializer());
        outputTopic = testDriver.createOutputTopic("result-topic", stringSerde.deserializer(), longSerde.deserializer());

        // pre-populate store
        store = testDriver.getKeyValueStore("aggStore");
        store.put("a", 21L);
    }

    @AfterEach
    public void tearDown() {
        testDriver.close();
    }

    @Test
    public void shouldFlushStoreForFirstInput() {
        inputTopic.pipeInput("a", 1L);
        assertEquals(KeyValue.pair("a", 21L), outputTopic.readKeyValue());
        assertTrue(outputTopic.isEmpty());
    }

    @Test
    public void shouldNotUpdateStoreForSmallerValue() {
        inputTopic.pipeInput("a", 1L);
        assertEquals(21L, store.get("a"));
        assertEquals(KeyValue.pair("a", 21L), outputTopic.readKeyValue());
        assertTrue(outputTopic.isEmpty());
    }

    @Test
    public void shouldUpdateStoreForLargerValue() {
        inputTopic.pipeInput("a", 42L);
        assertEquals(42L, store.get("a"));
        assertEquals(KeyValue.pair("a", 42L), outputTopic.readKeyValue());
        assertTrue(outputTopic.isEmpty());
    }

    @Test
    public void shouldUpdateStoreForNewKey() {
        inputTopic.pipeInput("b", 21L);
        assertEquals(21L, store.get("b"));
        assertEquals(KeyValue.pair("a", 21L), outputTopic.readKeyValue());
        assertEquals(KeyValue.pair("b", 21L), outputTopic.readKeyValue());
        assertTrue(outputTopic.isEmpty());
    }

    @Test
    public void shouldPunctuateIfStreamTimeAdvances() {
        var recordTime = Instant.now();
        inputTopic.pipeInput("a", 1L, recordTime);
        assertEquals(KeyValue.pair("a", 21L), outputTopic.readKeyValue());

        inputTopic.pipeInput("a", 1L, recordTime);
        assertTrue(outputTopic.isEmpty());

        inputTopic.pipeInput("a", 1L, recordTime.plusSeconds(10));
        assertEquals(KeyValue.pair("a", 21L), outputTopic.readKeyValue());
        assertTrue(outputTopic.isEmpty());
    }

    @Test
    public void shouldPunctuateIfWallClockTimeAdvances() {
        testDriver.advanceWallClockTime(Duration.ofSeconds(60));
        assertEquals(KeyValue.pair("a", 21L), outputTopic.readKeyValue());
        assertTrue(outputTopic.isEmpty());
    }

    static class CustomMaxAggregatorSupplier implements ProcessorSupplier<String, Long, String, Long> {
        @Override
        public Processor<String, Long, String, Long> get() {
            return new CustomMaxAggregator();
        }
    }

    static class CustomMaxAggregator extends ContextualProcessor<String, Long, String, Long> {

        private KeyValueStore<String, Long> store;

        @Override
        public void init(ProcessorContext<String, Long> context) {
            super.init(context);
            context.schedule(Duration.ofSeconds(60), WALL_CLOCK_TIME, this::flushStore);
            context.schedule(Duration.ofSeconds(10), STREAM_TIME, this::flushStore);
            store = context.getStateStore("aggStore");
        }

        @Override
        public void process(Record<String, Long> record) {
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
