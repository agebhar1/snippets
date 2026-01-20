package io.github.agebhar1.snippets.quarkus.control;

import io.github.agebhar1.snippets.quarkus.entity.StateChangeMessage;

public interface EventBusBridge {

    void publish(StateChangeMessage message);

}
