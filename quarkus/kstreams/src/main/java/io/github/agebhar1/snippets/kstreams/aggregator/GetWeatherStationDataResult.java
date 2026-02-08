package io.github.agebhar1.snippets.kstreams.aggregator;

import java.util.Optional;

public class GetWeatherStationDataResult {

    private static final GetWeatherStationDataResult NOT_FOUND = new GetWeatherStationDataResult(null);

    private final WeatherStationData result;

    private GetWeatherStationDataResult(WeatherStationData result) {
        this.result = result;
    }

    public static GetWeatherStationDataResult found(WeatherStationData data) {
        return new GetWeatherStationDataResult(data);
    }

    public static GetWeatherStationDataResult notFound() {
        return NOT_FOUND;
    }

    public Optional<WeatherStationData> getWeatherStationData() {
        return Optional.ofNullable(result);
    }

}
