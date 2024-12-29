package io.github.plantaest.citron.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WikiPageResponse(
        long id,
        String title,
        @JsonProperty("content_model")
        String contentModel,
        String source
) {}
