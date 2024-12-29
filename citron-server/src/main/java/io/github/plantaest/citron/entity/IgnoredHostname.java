package io.github.plantaest.citron.entity;

import io.github.plantaest.citron.config.recordbuilder.Builder;

import java.time.Instant;

@Builder
public record IgnoredHostname(
        long id,
        Instant createdAt,
        String wikiId,
        String hostname
) {}
