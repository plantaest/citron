package io.github.plantaest.citron.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WikiRevisionResponse(
        long id,
        @JsonProperty("content_model")
        String contentModel,
        String source
) {}
