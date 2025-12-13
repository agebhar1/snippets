package io.github.agebhar1.snippets.quarkus.fruit.control;

import io.github.agebhar1.snippets.quarkus.fruit.entity.Fruit;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.quarkiverse.loggingjson.providers.KeyValueStructuredArgument.kv;
import static java.util.Collections.unmodifiableCollection;

@ApplicationScoped
public class InMemoryFruitRepository implements FruitRepository {

    private static final Logger logger = Logger.getLogger(InMemoryFruitRepository.class);

    private final Map<Long, Fruit> data = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    @Override
    public Iterable<Fruit> findAll() {
        logger.debug("findAll()");
        return unmodifiableCollection(data.values());
    }

    @Override
    public Fruit findById(long primaryKey) {
        logger.debugf("findById", kv("primaryKey", primaryKey));
        return data.get(primaryKey);
    }

    @Override
    public Fruit save(Fruit entity) {
        logger.debugf("save", kv("entity", entity));
        entity = entity.withId(ids.getAndIncrement());
        data.put(entity.id(), entity);

        return entity;
    }

    @Override
    public void deleteById(long primaryKey) {
        logger.debugf("deleteById", kv("primaryKey", primaryKey));
        data.remove(primaryKey);
    }

    @Override
    public boolean existsById(long primaryKey) {
        logger.debugf("existsById", kv("primaryKey", primaryKey));
        return data.containsKey(primaryKey);
    }
}
