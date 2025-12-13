package io.github.agebhar1.snippets.quarkus.health.boundary;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

@Startup
@ApplicationScoped
public class ApplicationStartup implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("application startup");
    }
}
