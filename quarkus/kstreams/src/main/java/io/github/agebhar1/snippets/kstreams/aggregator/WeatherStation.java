package io.github.agebhar1.snippets.kstreams.aggregator;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WeatherStation(int id, String name) {
}
