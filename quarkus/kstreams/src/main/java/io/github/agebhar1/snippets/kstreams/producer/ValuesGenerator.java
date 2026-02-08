package io.github.agebhar1.snippets.kstreams.producer;

import io.github.agebhar1.snippets.kstreams.entity.Ping;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@ApplicationScoped
public class ValuesGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ValuesGenerator.class);

    private record WeatherStation(int id, String name, int averageTemperature) {
    }

    private final Random random = new Random();
    private final List<WeatherStation> stations = List.of(
            new WeatherStation(1, "Hamburg", 13),
            new WeatherStation(2, "Snowdonia", 5),
            new WeatherStation(3, "Boston", 11),
            new WeatherStation(4, "Tokio", 16),
            new WeatherStation(5, "Cusco", 12),
            new WeatherStation(6, "Svalbard", -7),
            new WeatherStation(7, "Porthsmouth", 11),
            new WeatherStation(8, "Oslo", 7),
            new WeatherStation(9, "Marrakesh", 20)
    );

    // @Outgoing("temperature-values")
    public Multi<Record<Integer, String>> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(1500))
                .onOverflow().drop()
                .map(tick -> {
                    var station = stations.get(random.nextInt(stations.size()));
                    var temperature = BigDecimal.valueOf(random.nextGaussian() * 15 + station.averageTemperature)
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();

                    logger.info("station {}, temperature: {}", station.name, temperature);
                    return Record.of(station.id, Instant.now() + ";" + temperature);
                });
    }

    // @Outgoing("weather-stations")
    public Multi<Record<Integer, String>> weatherStations() {
        return Multi.createFrom().items(stations.stream()
                .map(station -> Record.of(station.id, "{\"id\":" + station.id + ",\"name\":\"" + station.name + "\"}")));
    }

    @Outgoing("ping")
    public Multi<Record<String, Ping>> ping() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(1500))
                .onOverflow().drop()
                .map(tick -> {
//                    if (tick / 20 == 1) {
//                        return Record.of(String.valueOf(tick % 10), null);
//                    }
                    return Record.of(String.valueOf(tick % 10), new Ping(String.format("tick: %d", tick)));
                });
    }

}
