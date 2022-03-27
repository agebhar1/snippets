```
native-image -cp target/aws-sqs-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.agebhar1.App \
	--no-fallback \
	--initialize-at-build-time=org.slf4j.impl.SimpleLogger \
	--initialize-at-build-time=org.slf4j.impl.StaticLoggerBinder \
	--initialize-at-build-time=org.slf4j.LoggerFactory \
	--initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger \
	--initialize-at-run-time=io.netty.util.AbstractReferenceCounted \
	--initialize-at-run-time=io.netty.channel.epoll \
	--initialize-at-run-time=io.netty.handler.ssl \
	--initialize-at-run-time=io.netty.channel.unix
```
