package io.github.agebhar1.snippets;

import io.vertx.uritemplate.UriTemplate;
import io.vertx.uritemplate.Variables;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.reactive.common.util.PathHelper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTest {

    @Test // ~ RFC 6570 - Level 1
    void jakarta() {
        var builder = UriBuilder.fromUri("/{a}/{b}/{c}");
        var map = Map.of("a", "1", "b", "2.1/2.2", "c", "x y z");

        var result = builder.buildFromMap(map, false);

        assertThat(result.getHost()).isNullOrEmpty();
        assertThat(result.getScheme()).isNullOrEmpty();
        assertThat(result.getPort()).isEqualTo(-1);
        assertThat(result.getRawQuery()).isNullOrEmpty();

        assertThat(result.toASCIIString()).isEqualTo("/1/2.1/2.2/x%20y%20z");
    }

    @Test
    void params() {
        var matcher = PathHelper.URI_PARAM_PATTERN.matcher("/{a.x}/{b.y}/{a.x}");

        var params = new ArrayList<>();
        while (matcher.find()) {
            var group = matcher.group();
            params.add(group.substring(1, group.length() - 1));
        }

        assertThat(params).containsExactly("a.x", "b.y", "a.x");
    }

    @Test
    void rfc6570() {
        var template = UriTemplate.of("/{a}/{+b}/{c}");
        var variables = Variables.variables().set("a", "1").set("b", "2.1/2.2").set("c", "x y z");

        assertThat(template.expandToString(variables)).isEqualTo("/1/2.1/2.2/x%20y%20z");
    }

}
