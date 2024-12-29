package io.github.plantaest.citron.entity;

import io.github.plantaest.citron.config.recordbuilder.Builder;

import java.time.Instant;

@Builder
public record ReportedHostname(
        long id,
        Instant createdAt,
        String wikiId,
        String user,
        String page,
        long revisionId,
        long revisionTimestamp,
        String hostname,
        double score,
        int modelNumber
) {}
