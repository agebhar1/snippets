package io.github.agebhar1.snippets;

import io.vertx.uritemplate.UriTemplate;
import io.vertx.uritemplate.Variables;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTest {

    @Test
    void jakarta() {
        var builder = UriBuilder.fromUri("/{a}/{b}/{c}");
        var map = Map.of("a", "1", "b", "2.1/2.2", "c", "x y z");

        assertThat(builder.buildFromMap(map, false).toASCIIString()).isEqualTo("/1/2.1/2.2/x%20y%20z");
    }

    @Test
    void rfc6570() {
        var template = UriTemplate.of("/{a}/{+b}/{c}");
        var variables = Variables.variables().set("a", "1").set("b", "2.1/2.2").set("c", "x y z");

        assertThat(template.expandToString(variables)).isEqualTo("/1/2.1/2.2/x%20y%20z");
    }

}
