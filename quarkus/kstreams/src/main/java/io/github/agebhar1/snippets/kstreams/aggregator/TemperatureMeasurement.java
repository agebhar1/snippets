package io.github.agebhar1.snippets.kstreams.aggregator;

import java.time.Instant;

public record TemperatureMeasurement(WeatherStation station, Instant timestamp, double value) {
}
