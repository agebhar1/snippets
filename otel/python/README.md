```shell
python3 -m venv venv
pip3 install -r requirements
# opentelemetry-bootstrap -a requirements
opentelemetry-bootstrap -a install
podman compose up -d
open http://localhost:16686
```

```shell
#export OTEL_PYTHON_LOG_CORRELATION=true
#export OTEL_PYTHON_LOG_FORMAT="%(msg)s [span_id=%(span_id)s]"
#export OTEL_PYTHON_LOG_LEVEL=debug
#export OTEL_PYTHON_LOGGING_AUTO_INSTRUMENTATION_ENABLED=true
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
opentelemetry-instrument --traces_exporter console,otlp --service_name app python3 server.py
```

```shell
curl http://localhost:8000/
```

## Links

### Blogs

* https://signoz.io/blog/opentelemetry-fastapi/
* https://last9.io/blog/integrating-opentelemetry-with-fastapi/
* https://medium.com/@lakinduboteju/integrating-opentelemetry-for-logging-in-python-a-practical-guide-fe52bff61edc

### OpenTelemetry

* [Logging Library SDK Prototype Specification](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.47.0/oteps/logs/0150-logging-library-sdk.md)

#### Collector

* [Resiliency](https://opentelemetry.io/docs/collector/resiliency/)

#### Language APIs & SDKs

* https://opentelemetry.io/docs/languages/sdk-configuration/otlp-exporter/
* https://opentelemetry.io/docs/zero-code/python/configuration/
* https://opentelemetry.io/docs/zero-code/python/troubleshooting/#connectivity-issues

#### GitHub (Python)

* https://github.com/open-telemetry/opentelemetry-python/tree/main/docs/examples/auto-instrumentation

### OpenTelemetry-Python-Contrib

```shell
pip install opentelemetry-exporter-{exporter}
pip install opentelemetry-instrumentation-{instrumentation}
pip install opentelemetry-sdk-extension-{sdk-extension}
```

#### Instrumentations

* [Confluent Kafka](https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/confluent_kafka/confluent_kafka.html)
* [FastAPI](https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/fastapi/fastapi.html)
* [Logging](https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/logging/logging.html)
* [SQLAlchemy](https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/sqlalchemy/sqlalchemy.html)
