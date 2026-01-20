# Run

```
$ podman compose up
[+] up 4/4
 ✔ Network quarkus-listen_default    Created                                                                                                                                                                                              0.0s
 ✔ Container quarkus-listen-valkey-1 Created                                                                                                                                                                                              1.5s
 ✔ Container quarkus-listen-listen-2 Created                                                                                                                                                                                              1.5s
 ✔ Container quarkus-listen-listen-1 Created                                                                                                                                                                                              1.5s
Attaching to listen-1, listen-2, valkey-1
valkey-1  | 1:M 25 Jan 2026 11:42:55.726 # WARNING Memory overcommit must be enabled! Without it, a background save or replication may fail under low memory condition. Being disabled, it can also cause failures without low memory condition, see https://github.com/jemalloc/jemalloc/issues/1328. To fix this issue add 'vm.overcommit_memory = 1' to /etc/sysctl.conf and then reboot or run the command 'sysctl vm.overcommit_memory=1' for this to take effect.
valkey-1  | 1:M 25 Jan 2026 11:42:55.726 * oO0OoO0OoO0Oo Valkey is starting oO0OoO0OoO0Oo
valkey-1  | 1:M 25 Jan 2026 11:42:55.726 * Valkey version=9.0.1, bits=64, commit=00000000, modified=0, pid=1, just started
valkey-1  | 1:M 25 Jan 2026 11:42:55.726 # Warning: no config file specified, using the default config. In order to specify a config file use valkey-server /path/to/valkey.conf
valkey-1  | 1:M 25 Jan 2026 11:42:55.727 * monotonic clock: POSIX clock_gettime
valkey-1  | 1:M 25 Jan 2026 11:42:55.728 * Running mode=standalone, port=6379.
valkey-1  | 1:M 25 Jan 2026 11:42:55.728 * Server initialized
valkey-1  | 1:M 25 Jan 2026 11:42:55.729 * Ready to accept connections tcp
listen-1  | INFO exec -a "java" java -XX:MaxRAMPercentage=80.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -cp "." -jar /deployments/quarkus-run.jar 
listen-1  | INFO running in /deployments
listen-2  | INFO exec -a "java" java -XX:MaxRAMPercentage=80.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -cp "." -jar /deployments/quarkus-run.jar 
listen-2  | INFO running in /deployments
listen-2  | __  ____  __  _____   ___  __ ____  ______ 
listen-2  |  --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
listen-2  |  -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
listen-2  | --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
listen-2  | 2026-01-25 11:42:56,749 INFO  [io.quarkus] (main) snippets-quarkus-listen 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.30.6) started in 0.849s. Listening on: http://0.0.0.0:8080
listen-2  | 2026-01-25 11:42:56,750 INFO  [io.quarkus] (main) Profile prod activated. 
listen-2  | 2026-01-25 11:42:56,750 INFO  [io.quarkus] (main) Installed features: [cdi, redis-client, rest, rest-jackson, smallrye-context-propagation, smallrye-health, vertx, websockets-next]
listen-1  | __  ____  __  _____   ___  __ ____  ______ 
listen-1  |  --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
listen-1  |  -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
listen-1  | --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
listen-1  | 2026-01-25 11:42:56,758 INFO  [io.quarkus] (main) snippets-quarkus-listen 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.30.6) started in 0.862s. Listening on: http://0.0.0.0:8080
listen-1  | 2026-01-25 11:42:56,759 INFO  [io.quarkus] (main) Profile prod activated. 
listen-1  | 2026-01-25 11:42:56,759 INFO  [io.quarkus] (main) Installed features: [cdi, redis-client, rest, rest-jackson, smallrye-context-propagation, smallrye-health, vertx, websockets-next]
```

```shell
$ for i in $(seq 2); do open http://localhost$(podman compose port listen 8080 --index $i); done
```

```shell
$ curl --silent -L -H "Content-Type: application/json" -d '{ "id": "1", "value": "a" }' http://localhost$(podman compose port listen 8080)/items | jq .
{
  "id": "1",
  "value": "a"
}
$ curl --silent http://localhost$(podman compose port listen 8080)/items | jq .
[
  {
    "id": "1",
    "value": "a"
  }
]

$ curl --silent -X PUT -H "Content-Type: application/json" -d '{ "id": "1", "value": "b" }' http://localhost$(podman compose port listen 8080)/items/1 | jq .
{
  "id": "1",
  "value": "b"
}
$ curl --silent http://localhost$(podman compose port listen 8080)/items | jq .
[
  {
    "id": "1",
    "value": "b"
  }
]
$ curl --silent -X DELETE http://localhost$(podman compose port listen 8080)/items/1
```

# Issues

* non-transactional

# Links

* [Using the Redis Client](https://quarkus.io/guides/redis)
* [Redis Extension Reference Guide](https://quarkus.io/guides/redis-reference)
* [Dev Services for Redis](https://quarkus.io/guides/redis-dev-services)
* [Valkey Pub/Sub](https://valkey.io/topics/pubsub/)
* [No More Polling: Build Real-Time Java Apps with Quarkus WebSocket Next](https://www.the-main-thread.com/p/real-time-java-quarkus-websockets-next)
* [WebSockets Next reference guide](https://quarkus.io/guides/websockets-next-reference)
  * [Configuration reference](https://quarkus.io/guides/websockets-next-reference#websocket-next-configuration-reference)
* [Redis Pub/Sub with Java using Jedis](https://medium.com/@techworldthink/redis-pub-sub-with-java-using-jedis-4ffb76e6d196)
* https://github.com/quarkusio/quarkus/issues/13887#issuecomment-748936906
* [WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket) (Mozilla Developer Network)
