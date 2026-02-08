package io.github.agebhar1.snippets.kstreams.aggregator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/weather-station")
public class WeatherStationEndpoint {

    private final InteractiveQueries queries;

    public WeatherStationEndpoint(final InteractiveQueries queries) {
        this.queries = queries;
    }

    @GET
    @Path("/data/{id}")
    public Response getWeatherStationData(@PathParam("id") int id) {
        return queries
                .getWeatherStationData(id)
                .getWeatherStationData()
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND.getStatusCode(), String.format("No data found for weather station %d", id)))
                .build();
    }

}
