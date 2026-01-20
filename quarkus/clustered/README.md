# Run

```
$ podman compose up [--build]
```

```shell
$ for i in $(seq 3); do open http://localhost$(podman compose port clustered 8080 --index $i); done
```

```shell
$ curl --silent -L -H "Content-Type: application/json" -d '{ "id": "'$(uuidgen)'", "value": "a" }' http://localhost$(podman compose port clustered 8080 --index 1)/items | jq .
```

# Notes

* non-transactional

# Links

## Quarkus

* [WebSockets Next reference guide](https://quarkus.io/guides/websockets-next-reference)
    * [Configuration reference](https://quarkus.io/guides/websockets-next-reference#websocket-next-configuration-reference)
* [Using Stork with Kubernetes](https://quarkus.io/guides/stork-kubernetes)
  * [Getting Started with SmallRye Stork](https://quarkus.io/guides/stork)
    * [Stork Reference Guide](https://quarkus.io/guides/stork-reference) 
    * [Smallrye Stork](https://smallrye.io/smallrye-stork/latest/) is a service discovery and client-side load-balancing framework.
* [Using the event bus](https://quarkus.io/guides/reactive-event-bus)
* [Vert.x Reference Guide](https://quarkus.io/guides/vertx-reference)
  * [Use the Event Bus](https://quarkus.io/guides/vertx-reference#eventbus)

## Quarkiverse

* [Quarkus Infinispan Embedded](https://docs.quarkiverse.io/quarkus-infinispan-embedded/dev/index.html)

## Vert.x

* [The Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus)
* [Clustered Event Bus](https://vertx.io/docs/vertx-core/java/#_clustered_event_bus)
* [Hazelcast Cluster Manager](https://vertx.io/docs/vertx-hazelcast/java/)
  * [Discovery options](https://vertx.io/docs/vertx-hazelcast/java/#_discovery_options)
  * [Configuring for Kubernetes](https://vertx.io/docs/vertx-hazelcast/java/#_configuring_for_kubernetes)
* [Infinispan Cluster Manager](https://vertx.io/docs/vertx-infinispan/java/)
* [Deploying clustered Vert.x apps on Kubernetes with Infinispan](https://vertx.io/docs/howtos/clustering-kubernetes-howto/)

## Hazelcast

* [Configuring Hazelcast in Embedded Mode](https://docs.hazelcast.com/hazelcast/5.6/configuration/configuring-programmatically)
* [Integrate with Vert.x](https://docs.hazelcast.com/hazelcast/5.6/integrate/integrate-with-vertx)

## Others

* [No More Polling: Build Real-Time Java Apps with Quarkus WebSocket Next](https://www.the-main-thread.com/p/real-time-java-quarkus-websockets-next)
* [WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket) (Mozilla Developer Network)
* [Easiest way to communicate between multiple instances of a Quarkus app? #42062](https://github.com/quarkusio/quarkus/discussions/42062)
* [Build embedded cache clusters with Quarkus and Red Hat Data Grid](https://developers.redhat.com/blog/2020/12/17/build-embedded-cache-clusters-with-quarkus-and-red-hat-data-grid#)
* [How do I configure Vert.x event bus to work across cluster of Docker containers?](https://stackoverflow.com/questions/39812848/how-do-i-configure-vert-x-event-bus-to-work-across-cluster-of-docker-containers)
