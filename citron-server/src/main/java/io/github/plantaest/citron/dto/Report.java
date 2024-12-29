package io.github.plantaest.citron.dto;

import io.github.plantaest.citron.config.recordbuilder.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record Report(
        int version,
        String updatedAt,
        List<Hostname> hostnames,
        Map<Long, Revision> revisions,
        List<Feedback> feedbacks
) {

    @Builder
    public record Hostname(
            String hostname,
            String time,
            double score,
            List<Long> revisionIds
    ) {}

    @Builder
    public record Revision(
            String page,
            String user
    ) {}

    @Builder
    public record Feedback(
            String createdAt,
            long createdBy,
            String hostname,
            int status,
            String hash,
            String user,
            boolean synced
    ) {}

}
