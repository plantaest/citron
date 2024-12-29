package io.github.plantaest.citron.config;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class Start {

    public void onStart(@Observes StartupEvent ev) {
        Log.debugf("Starting...");
    }

}
