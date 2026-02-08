package io.github.agebhar1.snippets.kstreams.aggregator;

import io.github.agebhar1.snippets.kstreams.TopologyProducer;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.util.Optional;

@ApplicationScoped
public class InteractiveQueries {

    private final KafkaStreams streams;

    public InteractiveQueries(KafkaStreams streams) {
        this.streams = streams;
    }

    public GetWeatherStationDataResult getWeatherStationData(int id) {
        return Optional.ofNullable(getWeatherStationStore().get(id))
                .map(data -> GetWeatherStationDataResult.found(WeatherStationData.from(data)))
                .orElse(GetWeatherStationDataResult.notFound());
    }

    private ReadOnlyKeyValueStore<Integer, TemperatureAggregation> getWeatherStationStore() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(TopologyProducer.WEATHER_STATIONS_STORE, QueryableStoreTypes.keyValueStore()));
            } catch (InvalidStateStoreException ignored) {
                // ignore, store not ready
            }
        }
    }


}
