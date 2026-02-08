package io.github.agebhar1.snippets.kstreams;

import io.github.agebhar1.snippets.kstreams.processor.Aggregator;
import io.github.agebhar1.snippets.kstreams.processor.Sender;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

import java.util.ArrayList;

import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
import static org.apache.kafka.streams.state.Stores.keyValueStoreBuilder;
import static org.apache.kafka.streams.state.Stores.persistentKeyValueStore;

@ApplicationScoped
public class TopologyProducer {

    static public class PingList extends ArrayList<Ping> {
        public PingList() {
            super();
        }
    }

    // https://github.com/quarkusio/quarkus/pull/18379/changes
    @Produces
    public StreamsUncaughtExceptionHandler getUncaughtExceptionHandler() {
        return exception -> REPLACE_THREAD;
    }

    @Produces
    public Topology topology() {

        var storeBuilder = keyValueStoreBuilder(
                persistentKeyValueStore("ping-store"),
                Serdes.String(),
                Serdes.ListSerde(PingList.class, new ObjectMapperSerde<>(Ping.class)));

        return new Topology()
                .addSource("s1", new StringDeserializer(), new ObjectMapperDeserializer<>(Ping.class), "ping")
                .addProcessor("aggregate", () -> new Aggregator(storeBuilder.name()), "s1")
                .addProcessor("send", Sender::new, "aggregate")
                .addStateStore(storeBuilder, "aggregate");
    }

}
