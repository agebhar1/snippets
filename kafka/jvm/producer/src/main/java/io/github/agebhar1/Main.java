package io.github.agebhar1;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private final static String topic = "test";

    public static void administer() {


    }

    public static void scheduleAtFixedRate(KafkaProducer<String, String> producer, long period, TimeUnit unit) {

        final var executorService = Executors.newScheduledThreadPool(1);

        final var counter = new AtomicInteger(1);

        final Runnable command = () -> {

            final var key = String.format("key-%d", counter.getAndIncrement());
            final var value = Instant.now().atZone(UTC).format(ISO_OFFSET_DATE_TIME);
            counter.compareAndExchange(4, 1);

            final var record = new ProducerRecord<>(topic, key, value);
            try {
                producer.send(record, (metadata, e) -> {
                    if (e != null) {
                        logger.error("Got exception while send record to Kafka", e);
                        return;
                    }
                    logger.info("Record: {}, Metadata: offset={}, timestamp={}, serializedKeySize={}, serializedValueSize={}, partition={}",
                            record,
                            metadata.offset(), metadata.timestamp(), metadata.serializedKeySize(),
                            metadata.serializedValueSize(), metadata.partition());
                });
            } catch (final Exception e) {
                logger.error("Got exception while send record top Kafka", e);
            }
        };
        final var job = executorService.scheduleAtFixedRate(command, 0, period, unit);
        final var shutdown = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Start exit...");
            job.cancel(true);
            executorService.shutdown();
            shutdown.countDown();
        }));

        try {
            shutdown.await();
        } catch (final InterruptedException ignore) {

        }
        producer.close();

    }

    public static void sized(KafkaProducer<String, String> producer, int limit, boolean async) {
        var latch = new CountDownLatch(limit);
        for (var counter = 0; counter < limit; counter++) {

            var key = String.format("key-%d", counter % 3);
            var value = Integer.toString(counter);

            var record = new ProducerRecord<>(topic, key, value);
            try {
                var future = producer.send(record, (metadata, e) -> {
                    latch.countDown();
                    if (e != null) {
                        logger.error("(Callback) Got exception while send record to Kafka", e);
                    }
                });
                if (!async) {
                    future.get();
                }
            } catch (final Exception e) {
                logger.error("(Loop) Got exception while send record top Kafka", e);
            }
        }
        try {
            latch.await();
        } catch (InterruptedException ignore) {
        }
    }

    public static void main(String[] args) {

        final var properties = new Properties();

        properties.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,localhost:29092,localhost:39092");
        properties.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        properties.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        properties.put(ACKS_CONFIG, "all");
        properties.put(RETRIES_CONFIG, "2");

        final var producer = new KafkaProducer<String, String>(properties);

        // scheduleAtFixedRate(producer, 100, MILLISECONDS);

        var start = System.nanoTime();
        sized(producer, Integer.parseInt(System.getenv("LIMIT")), !"sync".equalsIgnoreCase(System.getenv("MODE")));
        long finish = System.nanoTime();

        logger.info("elapsed: {} ms", (finish - start) / 1000000.0);
    }

}