package io.github.agebhar1.snippets.quarkus.control;

import io.github.agebhar1.snippets.quarkus.entity.Item;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ItemRepository {

    private final Map<String, Item> data = new HashMap<>();
    private final EventBus eventBus;

    public ItemRepository(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public Iterable<Item> findAll() {
        return data.values();
    }

    public Optional<Item> findByIdOptional(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public Optional<Item> add(Item item) {
        if (data.containsKey(item.id())) {
            return Optional.empty();
        }
        data.put(item.id(), item);

        eventBus.publish("StateChange", String.format("added: %s", item.id()));

        return Optional.of(item);
    }

    public Optional<Item> update(String id, Item item) {
        if (!data.containsKey(id)) {
            return Optional.empty();
        }
        data.put(id, item);

        eventBus.publish("StateChange", String.format("updated: %s", item.id()));

        return Optional.of(item);
    }

    public boolean deleteById(String id) {
        var deleted = data.remove(id) != null;
        if (deleted) {
            eventBus.publish("StateChange", String.format("deleted: %s", id));
        }
        return deleted;
    }

}
