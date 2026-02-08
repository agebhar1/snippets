package io.github.agebhar1.snippets.kstreams;

import io.github.agebhar1.snippets.kstreams.aggregator.TemperatureAggregation;
import io.github.agebhar1.snippets.kstreams.aggregator.TemperatureMeasurement;
import io.github.agebhar1.snippets.kstreams.aggregator.WeatherStation;
import io.github.agebhar1.snippets.kstreams.entity.Ping;
import io.github.agebhar1.snippets.kstreams.processors.PingProcessor;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.kafka.streams.processor.PunctuationType.WALL_CLOCK_TIME;
import static org.apache.kafka.streams.state.Stores.keyValueStoreBuilder;
import static org.apache.kafka.streams.state.Stores.persistentKeyValueStore;

@ApplicationScoped
public class TopologyProducer {

    private static final Logger logger = LoggerFactory.getLogger(TopologyProducer.class);

    public static final String WEATHER_STATIONS_STORE = "weather-stations-store";
    private static final String WEATHER_STATIONS_TOPIC = "weather-stations";
    private static final String TEMPERATURE_VALUES_TOPIC = "temperature-values";
    private static final String TEMPERATURES_AGGREGATED_TOPIC = "temperatures-aggregated";

    // @Produces
    public Topology buildTopologyStream() {

        var builder = new StreamsBuilder();

        var weatherStationSerde = new ObjectMapperSerde<>(WeatherStation.class);
        var aggregationSerde = new ObjectMapperSerde<>(TemperatureAggregation.class);
        var storeSupplier = persistentKeyValueStore(WEATHER_STATIONS_STORE);
        var stations = builder.globalTable(WEATHER_STATIONS_TOPIC, Consumed.with(Serdes.Integer(), weatherStationSerde));

        builder.stream(TEMPERATURE_VALUES_TOPIC, Consumed.with(Serdes.Integer(), Serdes.String()))
                .join(
                        stations,
                        (stationId, timestampAndValue) -> stationId,
                        (timestampAndValue, station) -> {
                            logger.info("Join: {}, {}", timestampAndValue, station);

                            var parts = timestampAndValue.split(";");
                            return new TemperatureMeasurement(station, Instant.parse(parts[0]), Double.parseDouble(parts[1]));
                        })
                .groupByKey()
                .aggregate(
                        () -> {
                            logger.info("New");
                            return new TemperatureAggregation();
                        },
                        (stationId, value, aggregation) -> {
                            logger.info("Aggregate: {}, {}, {}", stationId, value, aggregation);
                            return aggregation.updateBy(value);
                        },
                        Materialized.<Integer, TemperatureAggregation>as(storeSupplier)
                                .withKeySerde(Serdes.Integer())
                                .withValueSerde(aggregationSerde))
        ;
//                .toStream()
//                .peek((key, value) -> logger.info("Key: {}, Value: {}", key, value))
//                .to(TEMPERATURES_AGGREGATED_TOPIC, Produced.with(Serdes.Integer(), aggregationSerde));

        return builder.build();
    }

    static public class PingList extends ArrayList<Ping> {
        public PingList() {
            super();
        }
    }

    // @Produces
    public Topology buildTopologyGlobalStore() {

        var storeBuilder = keyValueStoreBuilder(
                persistentKeyValueStore("ping-store"),
                Serdes.String(),
                Serdes.ListSerde(PingList.class, new ObjectMapperSerde<>(Ping.class)));

        return (new Topology())
                .addGlobalStore(
                        storeBuilder,
                        "source-name",
                        new StringDeserializer(),
                        new ObjectMapperDeserializer<>(Ping.class),
                        "ping",
                        "processor-name",
                        () -> new PingProcessor(storeBuilder.name()))
                .addSource("s",
                        new StringDeserializer(),
                        new ObjectMapperDeserializer<>(Ping.class),
                        "other")
                .addProcessor("p", () -> new ContextualProcessor<String, Ping, String, Ping>() {

                    private KeyValueStore<String, List<Ping>> store;

                    @Override
                    public void init(ProcessorContext<String, Ping> context) {
                        super.init(context);
                        store = context.getStateStore(storeBuilder.name());
                        context.schedule(Duration.ofSeconds(5), WALL_CLOCK_TIME, (timestamp) -> {
                            logger.info("Periodic operation at: {}", timestamp);
                            try (var all = store.all()) {
                                all.forEachRemaining(value -> {
                                    logger.info("[{}] {} -> {}", timestamp, value.key, value.value);
                                });
                            }
                        });
                    }

                    @Override
                    public void process(Record<String, Ping> record) {

                    }

                }, "s");

    }

    @Produces
    public Topology buildTopologyLocalStore() {

        var storeBuilder = keyValueStoreBuilder(
                persistentKeyValueStore("ping-store"),
                Serdes.String(),
                Serdes.ListSerde(PingList.class, new ObjectMapperSerde<>(Ping.class)));

//        var props = new Properties();
//        var config = new TopologyConfig(new StreamsConfig(props));

        return (new Topology())
                .addSource("s1",
                        new StringDeserializer(),
                        new ObjectMapperDeserializer<>(Ping.class),
                        "ping")
                .addProcessor("aggregate",
                        () -> new ContextualProcessor<String, Ping, String, List<Ping>>() {

                            private KeyValueStore<String, List<Ping>> store;

                            @Override
                            public void init(ProcessorContext<String, List<Ping>> context) {
                                super.init(context);
                                store = context.getStateStore(storeBuilder.name());
                                context.schedule(Duration.ofSeconds(30), WALL_CLOCK_TIME, (timestamp) -> {
                                    logger.info("Periodic operation at: {}", timestamp);
                                    var keys = new ArrayList<String>();
                                    try (var all = store.all()) {
                                        all.forEachRemaining(value -> {
                                            logger.info("[{}] {} -> {}", timestamp, value.key, value.value);
                                            context().forward(new Record<>(value.key, value.value, Instant.now().toEpochMilli()));
                                            keys.add(value.key);
                                        });
                                    }
                                    keys.forEach(store::delete);
                                });
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

                        }, "s1")
                .addSink("sink",
                        "ping-send",
                        new StringSerializer(),
                        new ObjectMapperSerializer<PingList>(),
                        "aggregate")
                .addSource("s2",
                        new StringDeserializer(),
                        new ObjectMapperDeserializer<>(PingList.class),
                        "ping-send")
                .addProcessor("send",
                        () -> new ContextualProcessor<String, List<Ping>, Void, Void>() {

                            @Override
                            public void process(Record<String, List<Ping>> record) {
                                logger.info("send [{}] {} -> {}", record.timestamp(), record.key(), record.value());


//                                if (record.key().equals("5")) {
//                                    throw new IllegalStateException("");
//                                }

                            }


                        }, "s2")
                .addStateStore(storeBuilder, "aggregate");
    }

}
