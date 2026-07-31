# -= IMPORTANT =-
# initialize must run before import any supported Python library
from opentelemetry.instrumentation.auto_instrumentation import initialize

initialize()

import sys
from opentelemetry import trace
from confluent_kafka import Producer, Consumer, KafkaError, KafkaException

producer = Producer({'bootstrap.servers': "localhost:9092"})
consumer = Consumer({'bootstrap.servers': "localhost:9092", 'group.id': "foo", 'auto.offset.reset': 'smallest'})


def process(message):
    print(f"{message.topic()} [{message.partition(), message.offset()}]: {message.value().decode('utf-8')}")


def basic_consume_loop(consumer, topics):
    try:
        consumer.subscribe(topics)
        running = True
        while running:
            message = consumer.poll(timeout=1.0)
            # Span name: <Topic> process
            span = trace.get_current_span()
            # span.update_name('span name')
            span.set_attribute('user', 'X123')

            if message is None: continue
            if message.error():
                if message.error().code() == KafkaError._PARTITION_EOF:
                    # End of partition event
                    sys.stderr.write(
                        f"{message.topic()} [{message.partition()}] reached end at offset {message.offset()}")
                elif message.error():
                    raise KafkaException(message.error())
            else:
                process(message)
                # Span name: <Topic> send
                producer.produce('two', message.value())
    finally:
        # Close down consumer to commit final offsets.
        consumer.close()


basic_consume_loop(consumer, ["one"])
