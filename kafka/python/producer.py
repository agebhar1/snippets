from confluent_kafka import Producer
from datetime import timedelta
import os
from timeit import default_timer as timer


def acked(err, msg):
    if err is not None:
        print(f"(Callback) Got exception while send record top Kafka: {err}")


is_async = (os.environ.get("MODE") or "").casefold() != "sync"
config = {
    'acks': 'all',
    'bootstrap.servers': "localhost:19092,localhost:29092,localhost:39092"
}
producer = Producer(config)
start = timer()

for idx in range(0, int(os.environ.get("LIMIT"))):
    key = idx % 3
    #value = str(idx) if key != 2 else str(idx)*1024
    value = str(idx)
    try:
        producer.produce("test", key=str(key), value=value, callback=acked)
        if not is_async:
            producer.flush(1)
        producer.poll(0)
    except Exception as ex:
        print(f"(Loop) Got exception while send record top Kafka: {ex}")

print("wait for flush…")
while producer.flush() > 0:
    pass

end = timer()
print(timedelta(seconds=end - start))
