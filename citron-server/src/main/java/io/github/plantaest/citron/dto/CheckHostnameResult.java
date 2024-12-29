package io.github.plantaest.citron.dto;

public record CheckHostnameResult(
        String hostname,
        boolean existed
) {}
