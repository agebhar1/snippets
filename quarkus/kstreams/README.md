# Links

## Kafka Streams

* [Processor API](https://kafka.apache.org/42/streams/developer-guide/processor-api/)
* [Testing a Streams Application](https://kafka.apache.org/42/streams/developer-guide/testing/)

## Quarkus

* [Apache Kafka Reference Guide ](https://quarkus.io/guides/kafka)
* [Using Apache Kafka Streams](https://quarkus.io/guides/kafka-streams)
* [Micrometer Metrics](https://quarkus.io/guides/telemetry-micrometer)

### Metrics

* [Using OpenTelemetry Metrics](https://quarkus.io/guides/opentelemetry-metrics) (Preview)
* [Micrometer and OpenTelemetry extension](https://quarkus.io/guides/telemetry-micrometer-to-opentelemetry/) (Preview)
    * [Automatic instrumentation](https://quarkus.io/guides/opentelemetry-metrics/#automatic-instrumentation)
* [Collect metrics using Micrometer](https://quarkus.io/guides/telemetry-micrometer-tutorial/)

Consumer lag depends on `kafka.max.poll.records` (default `500`).
https://kafka.apache.org/44/configuration/consumer-configs/#consumerconfigs_max.poll.records

```
curl --silent http://localhost:8080/q/metrics | grep ^kafka | sort | grep fetch
kafka_consumer_fetch_manager_bytes_consumed_rate{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} 0.0
kafka_consumer_fetch_manager_bytes_consumed_total{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} 1791.0
kafka_consumer_fetch_manager_fetch_latency_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1"} NaN
kafka_consumer_fetch_manager_fetch_latency_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1"} NaN
kafka_consumer_fetch_manager_fetch_latency_max{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1"} NaN
kafka_consumer_fetch_manager_fetch_latency_max{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1"} NaN
kafka_consumer_fetch_manager_fetch_rate{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1"} 0.0
kafka_consumer_fetch_manager_fetch_rate{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1"} 0.0
kafka_consumer_fetch_manager_fetch_size_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} NaN
kafka_consumer_fetch_manager_fetch_size_max{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} NaN
kafka_consumer_fetch_manager_fetch_throttle_time_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1"} NaN
kafka_consumer_fetch_manager_fetch_throttle_time_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1"} 0.0
kafka_consumer_fetch_manager_fetch_throttle_time_max{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1"} NaN
kafka_consumer_fetch_manager_fetch_throttle_time_max{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1"} 0.0
kafka_consumer_fetch_manager_fetch_total{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1"} 0.0
kafka_consumer_fetch_manager_fetch_total{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1"} 2.0
kafka_consumer_fetch_manager_preferred_read_replica{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StateUpdater-1-restore-consumer",kafka_version="4.2.1",partition="0",topic="abc-ping-store-changelog"} -1.0
kafka_consumer_fetch_manager_preferred_read_replica{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} -1.0
kafka_consumer_fetch_manager_records_consumed_rate{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} 0.0
kafka_consumer_fetch_manager_records_consumed_total{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} 60.0
kafka_consumer_fetch_manager_records_lag_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} NaN
kafka_consumer_fetch_manager_records_lag{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} 0.0
kafka_consumer_fetch_manager_records_lag_max{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} NaN
kafka_consumer_fetch_manager_records_lead_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} NaN
kafka_consumer_fetch_manager_records_lead{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} 1094.0
kafka_consumer_fetch_manager_records_lead_min{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",partition="0",topic="ping"} NaN
kafka_consumer_fetch_manager_records_per_request_avg{client_id="abc-bb165ca7-fcb6-465c-b0d2-cbc4f95f4668-StreamThread-1-consumer",kafka_version="4.2.1",topic="ping"} NaN
```

## Quarkiverse

* [Kafka Streams Processor](https://docs.quarkiverse.io/quarkus-kafka-streams-processor/dev/index.html)
    * [Stateful EDA](https://docs.quarkiverse.io/quarkus-kafka-streams-processor/dev/index.html#_stateful_eda) * https://github.com/quarkiverse/quarkus-kafka-streams-processor/tree/main/integration-tests/stateful
    * https://github.com/quarkiverse/quarkus-kafka-streams-processor/tree/main/integration-tests/stateful-global

## SmallRye

* [SmallRye Reactive Messaging » Connectors » Apache Kafka](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/3.4/kafka/kafka.html)

## Vert.x

* [Vert.x Kafka client](https://vertx.io/docs/vertx-kafka-client/java/)

## Others

* [Monitor the Consumer Lag in Apache Kafka](https://www.baeldung.com/java-kafka-consumer-lag) (baeldung.com)