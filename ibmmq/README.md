```bash
$ podman run --rm --detach \
    --env LICENSE=accept \
    --env MQ_APP_PASSWORD=passw0rd \
    --env MQ_QMGR_NAME=QM1 \
    --publish 1414:1414 \
    --publish 9443:9443 \
    icr.io/ibm-messaging/mq:9.3.1.0-r2
```

```bash
$ podman run --rm --detach \
    --env POSTGRES_PASSWORD=passw0rd \
    --publish 5432:5432 \
    postgres:15.1 --max_prepared_transactions=100
```

