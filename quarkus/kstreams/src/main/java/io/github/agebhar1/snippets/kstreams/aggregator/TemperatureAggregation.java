package io.github.agebhar1.snippets.kstreams.aggregator;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RegisterForReflection
public class TemperatureAggregation {

    private static final Logger logger = LoggerFactory.getLogger(TemperatureAggregation.class);

    public WeatherStation station;
    public double min = Double.MAX_VALUE;
    public double max = Double.MIN_VALUE;
    public int count;
    public double sum;

    public TemperatureAggregation() {
        logger.info("Constructing temperature aggregation");
    }

    public TemperatureAggregation updateBy(TemperatureMeasurement measurement) {
        logger.info("Updating count: {}, sum: {} by {}", count, sum, measurement);

        if (station == null) {
            station = measurement.station();
        }

        count++;
        sum += measurement.value();

        min = Math.min(min, measurement.value());
        max = Math.max(max, measurement.value());

        logger.info("Updated count: {}, sum: {}", count, sum);
        return this;
    }

    public double getAvg() {
        if (count == 0) {
            return Double.NaN;
        }
        return BigDecimal.valueOf(sum / count).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String toString() {
        return "TemperatureAggregation{" +
                "station=" + station +
                ", min=" + min +
                ", max=" + max +
                ", count=" + count +
                ", sum=" + sum +
                '}';
    }
}
