package io.github.agebhar1.snippets.quarkus.health.control;

import io.smallrye.health.event.SmallRyeHealthStatusChangeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class HealthObserver {

    private final Logger logger = LoggerFactory.getLogger(HealthObserver.class);

    public void observeHealthChange(@Observes @Default SmallRyeHealthStatusChangeEvent event) {
        logger.info("@Observes @Default {}", event);
    }

    public void observesReadinessChange(@Observes @Readiness SmallRyeHealthStatusChangeEvent event) {
        logger.info("@Observes @Readiness {}", event);
    }

    public void observesLLivenessChange(@Observes @Liveness SmallRyeHealthStatusChangeEvent event) {
        logger.info("@Observes @Liveness {}", event);
    }

}
