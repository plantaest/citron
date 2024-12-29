package io.github.plantaest.citron.helper;

import jakarta.annotation.Nullable;

public record DiffComparison(
        @Nullable
        String oldText,
        @Nullable
        String newText
) {}
