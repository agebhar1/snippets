package io.github.agebhar1.snippets.quarkus.fruit.boundary;

import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import io.github.agebhar1.snippets.quarkus.fruit.control.FruitRepository;
import io.github.agebhar1.snippets.quarkus.fruit.entity.Fruit;
import org.jboss.resteasy.reactive.RestResponse;

import static org.jboss.resteasy.reactive.RestResponse.noContent;
import static org.jboss.resteasy.reactive.RestResponse.seeOther;

@Path("/fruits")
public class FruitResource {

    private final FruitRepository repository;

    public FruitResource(FruitRepository repository) {
        this.repository = repository;
        this.repository.save(new Fruit(null, "Apple", "Winter fruit"));
        this.repository.save(new Fruit(null, "Pineapple", "Tropical fruit"));
    }

    @GET
    public Iterable<Fruit> list() {
        return repository.findAll();
    }

    @GET
    @Path("{id}")
    public Fruit byId(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException();
        }
        return repository.findById(id);
    }

    @POST
    public RestResponse<Fruit> add(@Valid Fruit fruit, @Context UriInfo uriInfo) {
        fruit = repository.save(fruit.withId(null));
        return seeOther(uriInfo.getAbsolutePathBuilder().path(Long.toString(fruit.id())).build());
    }

    @DELETE
    @Path("{id}")
    public RestResponse<Void> delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException();
        }
        repository.deleteById(id);
        return noContent();
    }

}
