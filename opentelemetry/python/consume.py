# -= IMPORTANT =-
# initialize must run before import any supported Python library
from opentelemetry.instrumentation.auto_instrumentation import initialize

initialize()

import signal
import sys
from typing import Optional

from confluent_kafka import Producer, Consumer, KafkaError, KafkaException, Message
from opentelemetry import trace


def acked(err: Optional[KafkaError], message: Message) -> None:
    value = (message.value() or bytes()).decode('utf-8')
    if err is not None:
        print(f"[Callback 'produce'] Failed to deliver topic: {message.topic()}, message.value: {value}: {str(err)}")
    else:
        print(
            f"[Callback 'produce'] Message produced: topic: {message.topic()}, partition: {message.partition()}, offset: {message.offset()} => message.value: {value}")


def commit_complete(err: Optional[KafkaError], partitions) -> None:
    if err is not None:
        print(f"[Callback 'on_commit'] {str(err)}")
    else:
        print(f"[Callback 'on_commit'] Committed partitions offsets: {str(partitions)}")


def process(message: Message):
    ctx = trace.get_current_span().get_span_context()
    value = (message.value() or bytes()).decode('utf-8')
    print(
        f"[Consume] traceId: {'{0:x}'.format(ctx.trace_id)}, topic: {message.topic()}, partition: {message.partition()}, offset: {message.offset()} => message.value: {value}")


def basic_consume_loop(consumer: Consumer, producer: Producer, topics: list[str]) -> None:
    try:
        consumer.subscribe(topics)
        global running
        while running:
            message = consumer.poll(timeout=1.0)
            if message is None: continue
            if message.error():
                if message.error().code() == KafkaError._PARTITION_EOF:
                    # End of partition event
                    sys.stderr.write(
                        f"{message.topic()} [{message.partition()}] reached end at offset {message.offset()}")
                elif message.error():
                    raise KafkaException(message.error())
            else:
                # Span name: <Topic> process
                span = trace.get_current_span()
                # span.update_name('span name')
                span.set_attribute('user', 'X123')

                process(message)

                # Span name: <Topic> send
                topic = ""
                match message.topic():
                    case "one":
                        topic = "two"
                    case "two":
                        topic = "three"
                    case "three":
                        topic = "four"

                producer.produce(topic, message.value(), callback=acked)
                producer.poll(1)
    finally:
        # Close down consumer to commit final offsets.
        consumer.close()


running = True


def handle_signal(_signalnum, _handler):
    global running
    print("shutting down")
    running = False


signal.signal(signal.SIGINT, handle_signal)
signal.signal(signal.SIGTERM, handle_signal)

if __name__ == "__main__":
    producer = Producer({
        "bootstrap.servers": "localhost:9092"
    })
    consumer = Consumer({
        "bootstrap.servers": "localhost:9092",
        "group.id": "foo",
        "auto.offset.reset": "smallest",
        "on_commit": commit_complete
    })

    basic_consume_loop(consumer, producer, ["one", "two", "three"])
