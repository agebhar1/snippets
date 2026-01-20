package io.github.agebhar1.snippets.quarkus.config;

import io.quarkus.vertx.VertxOptionsCustomizer;
import io.vertx.core.VertxOptions;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

@ApplicationScoped
class Vertx implements VertxOptionsCustomizer {

    private final static Logger logger = LoggerFactory.getLogger(Vertx.class);

    @Override
    public void accept(VertxOptions options) {
        var host = getHostNameOrDefault(options.getEventBusOptions().getHost());
        logger.info("set (Quarkus) Vertx cluster host: {}", host); // overrides QUARKUS_VERTX_CLUSTER_HOST
        options.getEventBusOptions().setHost(host);
    }

    public static String getHostNameOrDefault(String defaultValue) {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
            return defaultValue;
        }
    }

}
