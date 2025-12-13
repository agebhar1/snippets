package org.acme.fruit.control;

import org.acme.fruit.entity.Fruit;

public interface FruitRepository {

    Iterable<Fruit> findAll();

    Fruit findById(long primaryKey);

    Fruit save(Fruit entity);

    void deleteById(long primaryKey);

    boolean existsById(long primaryKey);

}
