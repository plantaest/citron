package io.github.plantaest.citron.config.model;

import io.github.plantaest.citron.config.CitronConfig;
import io.github.plantaest.citron.enumeration.Model;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Startup
@Singleton
public class ModelManager {

    @Inject
    CitronConfig citronConfig;

    private final Map<String, byte[]> models = new ConcurrentHashMap<>();

    @PostConstruct
    void init() throws IOException {
        for (var model : citronConfig.models()) {
            try (var inputStream = getClass().getClassLoader().getResourceAsStream(model.path())) {
                if (inputStream != null) {
                    models.put(model.id(), inputStream.readAllBytes());
                }
            }
        }

    }

    public byte[] getModel(Model model) {
        return models.get(model.getId());
    }

}
