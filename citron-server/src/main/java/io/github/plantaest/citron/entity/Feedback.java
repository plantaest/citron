package io.github.plantaest.citron.entity;

import io.github.plantaest.citron.config.recordbuilder.Builder;

import java.time.Instant;

@Builder
public record Feedback(
        long id,
        Instant createdAt,
        long createdBy,
        String wikiId,
        String reportDate,
        String hostname,
        int status,
        String hash
) {}
