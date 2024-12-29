package io.github.plantaest.citron.helper.classifier;

import io.github.plantaest.citron.config.recordbuilder.Builder;

@Builder
public record ClassificationResult(
        String hostname,
        long label,
        float probability
) {}
