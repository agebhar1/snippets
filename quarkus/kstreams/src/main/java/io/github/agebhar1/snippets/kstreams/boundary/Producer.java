package io.github.agebhar1.snippets.kstreams.boundary;

import io.github.agebhar1.snippets.kstreams.Ping;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.concurrent.CompletionStage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/")
public class Producer {

    private final Emitter<Record<String, Ping>> emitter;

    public Producer(@Channel("ping") Emitter<Record<String, Ping>> emitter) {
        this.emitter = emitter;
    }

    // curl -v -H "Content-Type: application/json" -H "X-Key: abc" -d '{"message":"Hello"}' localhost:8080

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    public CompletionStage<Void> post(@NotNull @HeaderParam("X-Key") String key, @NotNull Ping payload) {
        return emitter.send(Record.of(key, payload));
    }

}
