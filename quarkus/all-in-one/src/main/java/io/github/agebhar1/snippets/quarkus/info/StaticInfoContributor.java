package io.github.agebhar1.snippets.quarkus.info;

import io.quarkus.info.runtime.spi.InfoContributor;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

@Singleton
public class StaticInfoContributor implements InfoContributor {

    private final Map<String, Object> data;

    public StaticInfoContributor(@ConfigProperty(name = "q.info.static") Optional<List<String>> data) {
        this.data = data
                .map(xs -> xs.stream()
                        .map(it -> it.split("\\s*=\\s*", 2))
                        .collect(toMap(kv -> kv[0], kv -> (Object) (kv.length == 2 ? kv[1] : "")))
                )
                .orElse(emptyMap());
    }

    @Override
    public String name() {
        return "static";
    }

    @Override
    public Map<String, Object> data() {
        return data;
    }

}
