package io.github.agebhar1.snippets.kstreams.boundary;

import io.github.agebhar1.snippets.kstreams.TopologyProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.HashMap;

import static org.apache.kafka.streams.StoreQueryParameters.fromNameAndType;
import static org.apache.kafka.streams.state.QueryableStoreTypes.keyValueStore;

@ApplicationScoped
@Path("/")
public class Queries {

    private final KafkaStreams streams;

    public Queries(KafkaStreams streams) {
        this.streams = streams;
    }

    private ReadOnlyKeyValueStore<String, TopologyProducer.PingList> getStore() {
        while (true) {
            try {
                return streams.store(fromNameAndType("ping-store", keyValueStore()));
            } catch (InvalidStateStoreException e) {
                // ignore, store not ready
            }
        }
    }

    @GET
    public RestResponse<Object> get() {
        var result = new HashMap<String, Object>();
        try (var iter = getStore().all()) {
            while (iter.hasNext()) {
                var kv = iter.next();
                result.put(kv.key, kv.value);
            }
        }

        return RestResponse.ok(result);
    }

}
