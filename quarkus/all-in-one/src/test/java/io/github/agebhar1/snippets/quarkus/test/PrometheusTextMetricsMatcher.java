package io.github.agebhar1.snippets.quarkus.test;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;

public class PrometheusTextMetricsMatcher extends TypeSafeMatcher<String> {

    private final ComparativeMetric[] expected;

    private PrometheusTextMetricsMatcher(ComparativeMetric[] expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(String item) {
        var actual = item.lines()
                .filter(line -> !line.startsWith("#"))
                .map(Metric::of)
                .toList();

        return Stream.of(expected).allMatch(metric ->
                actual.stream()
                        .filter(it -> it.metric.equals(metric.name))
                        .anyMatch(it -> it.labels == null ||
                                Stream.of(metric.labels).allMatch(label ->
                                        it.labels.contains(format("%s=\"%s\"", label.name, label.value)))
                        ));
    }

    @Override
    public void describeTo(Description description) {

    }

    public static PrometheusTextMetricsMatcher prometheusTextMetrics(ComparativeMetric... metrics) {
        return new PrometheusTextMetricsMatcher(metrics);
    }

    public static ComparativeMetric metric(String name, ComparativeMetricLabel... labels) {
        return new ComparativeMetric(name, labels);
    }

    public static ComparativeMetricLabel label(String name, String value) {
        return new ComparativeMetricLabel(name, value);
    }

    public static class ComparativeMetric {

        private final String name;
        private final ComparativeMetricLabel[] labels;

        ComparativeMetric(String name, ComparativeMetricLabel[] labels) {
            this.name = name;
            this.labels = labels;
        }

    }

    public static class ComparativeMetricLabel {

        private final String name;
        private final String value;

        ComparativeMetricLabel(String name, String value) {
            this.name = name;
            this.value = value;
        }

    }

    static class Metric {

        private static final String METRIC_NAME = "(?<metric>[a-zA-Z_:][a-zA-Z0-9_:]*)";
        private static final String LABEL = "[a-zA-Z_][a-zA-Z0-9_]*=\"[^\"]*\"";
        private static final String LABELS = LABEL + "(?:," + LABEL + ")*";
        private static final String VALUE = "(?<value>NaN|-?[0-9]+(?:.[0-9]+(?:E-?[0-9]+)?)?)";

        private final static Pattern METRIC = Pattern.compile(METRIC_NAME + "(?<labels>[{]" + LABELS + "})? " + VALUE);

        private final String metric;
        private final @Nullable String labels;
        private final String value;

        Metric(String metric, @Nullable String labels, String value) {
            this.metric = metric;
            this.labels = labels;
            this.value = value;
        }

        static Metric of(String value) {
            var m = METRIC.matcher(value);
            if (!m.matches()) {
                throw new IllegalArgumentException("Invalid metric: " + value);
            }
            return new Metric(m.group("metric"), m.group("labels"), m.group("value"));
        }

        @Override
        public String toString() {
            return "Metric{" +
                    "metric='" + metric + '\'' +
                    ", labels='" + labels + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

}
