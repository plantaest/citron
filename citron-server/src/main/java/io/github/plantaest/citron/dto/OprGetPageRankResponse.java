package io.github.plantaest.citron.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public record OprGetPageRankResponse(
        @JsonProperty("status_code")
        int statusCode,
        List<Response> response,
        @JsonProperty("last_updated")
        String lastUpdated
) {

    public record Response(
            @JsonProperty("status_code")
            int statusCode,
            String error,
            @JsonProperty("page_rank_integer")
            int pageRankInteger,
            @JsonProperty("page_rank_decimal")
            double pageRankDecimal,
            @Nullable
            String rank,
            String domain
    ) {}

}
