package io.github.plantaest.citron.helper;

import de.siegmar.fastcsv.reader.CsvReader;
import io.github.plantaest.citron.config.CitronConfig;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Startup
@Singleton
public class IgnoredSuffixes {

    @Inject
    CitronConfig citronConfig;

    private Set<String> ignoredSuffixes;

    @PostConstruct
    void init() throws IOException {
        try (var reader = CsvReader.builder()
                .ofCsvRecord(Helper.readFromClasspath(citronConfig.ignoredSuffixesFilePath()))) {
            ignoredSuffixes = reader.stream()
                    .map((record) -> record.getField(0))
                    .collect(Collectors.toSet());
        }
    }

    public boolean contains(String hostname) {
        String[] parts = hostname.split("\\.");

        if (parts.length >= 4) {
            String lastTwoParts = "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
            String lastThreeParts = "." + parts[parts.length - 3] + "." + parts[parts.length - 2]
                    + "." + parts[parts.length - 1];
            return ignoredSuffixes.contains(lastTwoParts) || ignoredSuffixes.contains(lastThreeParts);
        }

        if (parts.length == 3) {
            String lastTwoParts = "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
            return ignoredSuffixes.contains(lastTwoParts);
        }

        return false;
    }

}
