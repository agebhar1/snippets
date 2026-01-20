package io.github.agebhar1.snippets.quarkus.control;

import io.github.agebhar1.snippets.quarkus.entity.StateChangeMessage;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
public class RedisEventBusBridge implements EventBusBridge {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventBusBridge.class);

    private static final String EVENT = "StateChange";
    private static final String CHANNEL = "EventBusBridge.service." + EVENT;

    private final EventBus eventBus;
    private final PubSubCommands<StateChangeMessage> pubSub;

    public RedisEventBusBridge(RedisDataSource redis, EventBus eventBus) {
        this.eventBus = eventBus;

        this.pubSub = redis.pubsub(StateChangeMessage.class);
        this.pubSub.subscribe(CHANNEL, this::onRedisMessage);
    }

    private void onRedisMessage(StateChangeMessage message) {
        logger.info("Received message from channel: {}", message);
        eventBus.publish(EVENT, message);
    }

    @Override
    public void publish(StateChangeMessage message) {
        logger.info("Publishing message to channel: {}", message);
        pubSub.publish(CHANNEL, message);
    }

}
