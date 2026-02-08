package io.github.agebhar1.snippets.kstreams.processor;

import io.github.agebhar1.snippets.kstreams.Ping;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Sender extends ContextualProcessor<String, List<Ping>, Void, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Sender.class);

    @Override
    public void process(final Record<String, List<Ping>> record) {
        logger.info("send [{}] {} -> {}", record.timestamp(), record.key(), record.value());
//        if (record.key().equals("6")) {
//            throw new IllegalStateException("Intended exception");
//        }
    }

}
