package io.github.agebhar1.snippets.kstreams.aggregator;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WeatherStationData(WeatherStation station, double min, double max, double avg) {
    public static WeatherStationData from(TemperatureAggregation data) {
        return new WeatherStationData(data.station, data.min, data.max, data.getAvg());
    }
}
