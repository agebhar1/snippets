package io.github.agebhar1.snippets.kstreams;

import io.github.agebhar1.snippets.kstreams.processor.Aggregator;
import io.github.agebhar1.snippets.kstreams.processor.Sender;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;

import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;

@ApplicationScoped
public class TopologyProducer {

    private final boolean punctuationEnabled;

    static public class PingList extends ArrayList<Ping> {
        public PingList() {
            super();
        }
    }

    public TopologyProducer(@ConfigProperty(name = "app.punctuation.enabled") boolean punctuationEnabled) {
        this.punctuationEnabled = punctuationEnabled;
    }

    // https://github.com/quarkusio/quarkus/pull/18379/changes
    @Produces
    public StreamsUncaughtExceptionHandler getUncaughtExceptionHandler() {
        return exception -> REPLACE_THREAD;
    }

    @Produces
    public Topology topology() {

        return new Topology()
                .addSource("s1", new StringDeserializer(), new ObjectMapperDeserializer<>(Ping.class), "ping")
                .addProcessor("aggregate", Aggregator.supply(punctuationEnabled), "s1")
                .addProcessor("send", Sender::new, "aggregate");
                // .addStateStore(storeBuilder, "aggregate")
    }

}
