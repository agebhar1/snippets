package io.github.agebhar1.snippets.quarkus.fruit.control;

import io.github.agebhar1.snippets.quarkus.fruit.entity.Fruit;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;

import java.util.UUID;

public interface FruitRepository extends PanacheRepositoryBase<Fruit, UUID> {

    default Fruit update(UUID id, Fruit fruit) {
        // TODO check => id != fruit.id
        return findByIdOptional(id)
                .map(entity -> getEntityManager().merge(new Fruit(entity.getId(), fruit.getVersion(), fruit.getName(), fruit.getDescription(), entity.getMetadata())))
                .orElseThrow(NotFoundException::new);
    }

}
