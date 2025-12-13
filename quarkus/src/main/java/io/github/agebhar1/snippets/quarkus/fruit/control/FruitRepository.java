package io.github.agebhar1.snippets.quarkus.fruit.control;

import io.github.agebhar1.snippets.quarkus.fruit.entity.Fruit;

public interface FruitRepository {

    Iterable<Fruit> findAll();

    Fruit findById(long primaryKey);

    Fruit save(Fruit entity);

    void deleteById(long primaryKey);

    boolean existsById(long primaryKey);

}
