package org.acme.fruit.control;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.fruit.boundary.FruitResource;
import org.acme.fruit.entity.Fruit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.unmodifiableCollection;

@ApplicationScoped
public class InMemoryFruitRepository implements FruitRepository {

    private final Logger logger = LoggerFactory.getLogger(InMemoryFruitRepository.class);

    private final Map<Long, Fruit> data = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    @Override
    public Iterable<Fruit> findAll() {
        logger.debug("findAll()");
        return unmodifiableCollection(data.values());
    }

    @Override
    public Fruit findById(long primaryKey) {
        logger.debug("findById({})", primaryKey);
        return data.get(primaryKey);
    }

    @Override
    public Fruit save(Fruit entity) {
        logger.debug("save({})", entity);
        entity = entity.withId(ids.getAndIncrement());
        data.put(entity.id(), entity);

        return entity;
    }

    @Override
    public void deleteById(long primaryKey) {
        MDC.put("id", String.valueOf(primaryKey));
        logger.debug("deleteById({})", primaryKey);
        data.remove(primaryKey);
    }

    @Override
    public boolean existsById(long primaryKey) {
        MDC.put("id", String.valueOf(primaryKey));
        logger.debug("existsById({})", primaryKey);
        return data.containsKey(primaryKey);
    }
}
