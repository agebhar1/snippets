package io.github.agebhar1.snippets.quarkus.boundary;

import io.github.agebhar1.snippets.quarkus.control.ItemRepository;
import io.github.agebhar1.snippets.quarkus.entity.Item;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;

import static org.jboss.resteasy.reactive.RestResponse.noContent;
import static org.jboss.resteasy.reactive.RestResponse.ok;

@Path("/items")
public class ItemResource {

    private final ItemRepository repository;

    public ItemResource(ItemRepository repository) {
        this.repository = repository;
    }

    @GET
    public Iterable<Item> list() {
        return repository.findAll();
    }

    @GET
    @Path("/{id}")
    public RestResponse<Item> byId(@PathParam("id") String id) {
        return ok(repository.findByIdOptional(id).orElseThrow(NotFoundException::new));
    }

    @POST
    public RestResponse<Item> add(Item item, @Context UriInfo uriInfo) {
        return repository
                .add(item)
                .map(it -> RestResponse.<Item>seeOther(uriInfo.getAbsolutePathBuilder().path(it.id()).build()))
                .orElseThrow(BadRequestException::new);
    }

    @PUT
    @Path("/{id}")
    public RestResponse<Item> update(@PathParam("id") String id, Item item) {
        return ok(repository.update(id, item).orElseThrow(NotFoundException::new));
    }

    @DELETE
    @Path("/{id}")
    public RestResponse<Void> delete(@PathParam("id") String id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException();
        }
        return noContent();
    }

}
