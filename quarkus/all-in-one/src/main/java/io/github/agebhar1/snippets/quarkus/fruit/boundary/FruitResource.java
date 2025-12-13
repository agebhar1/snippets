package io.github.agebhar1.snippets.quarkus.fruit.boundary;

import io.github.agebhar1.snippets.quarkus.fruit.control.FruitRepository;
import io.github.agebhar1.snippets.quarkus.fruit.entity.Fruit;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.jboss.resteasy.reactive.RestResponse.noContent;
import static org.jboss.resteasy.reactive.RestResponse.ok;
import static org.jboss.resteasy.reactive.RestResponse.seeOther;

@Path("/fruits")
public class FruitResource {

    private final FruitRepository repository;

    public FruitResource(FruitRepository repository) {
        this.repository = repository;
    }

    @GET
    public Iterable<Fruit> list() {
        return repository.listAll();
    }

    @GET
    @Path("{id}")
    public RestResponse<Fruit> byId(@PathParam("id") UUID id) {
        return ok(repository.findByIdOptional(id).orElseThrow(NotFoundException::new));
    }

    @POST
    @Transactional
    public RestResponse<Fruit> add(@Valid Fruit fruit, @Context UriInfo uriInfo) {
        repository.persist(fruit);
        return seeOther(uriInfo.getAbsolutePathBuilder().path(fruit.getId().toString()).build());
    }

    @PUT
    @Path("{id}")
    @Transactional
    public RestResponse<Fruit> update(@PathParam("id") UUID id, @Valid Fruit fruit) {
        try {
            return ok(repository.update(id, fruit));
        } catch (final OptimisticLockException e) {
            throw new ClientErrorException(CONFLICT, e);
        }
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public RestResponse<Void> delete(@PathParam("id") UUID id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException();
        }
        return noContent();
    }

}
