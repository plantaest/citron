package io.github.plantaest.citron.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public record CompareRevisionsResponse(
        Revision from,
        Revision to,
        List<Diff> diff
) {

    public record Revision(
            long id,
            @JsonProperty("slot_role")
            String slotRole,
            List<Section> sections
    ) {

        public record Section(
                int level,
                String heading,
                int offset
        ) {}

    }

    public record Diff(
            int type,
            @Nullable
            Integer lineNumber,
            @Nullable
            MoveInfo moveInfo,
            String text,
            Offset offset,
            @Nullable
            List<HighlightRange> highlightRanges
    ) {

        public record Offset(
                @Nullable
                Integer from,
                @Nullable
                Integer to
        ) {}

        public record HighlightRange(
                int start,
                int length,
                int type
        ) {}

        public record MoveInfo(
                String id,
                String linkId,
                int linkDirection
        ) {}

    }

}
