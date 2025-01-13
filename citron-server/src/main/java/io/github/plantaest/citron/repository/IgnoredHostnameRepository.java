package io.github.plantaest.citron.repository;

import io.github.plantaest.citron.dto.CheckHostnameResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class IgnoredHostnameRepository {

    @Inject
    Jdbi jdbi;

    public List<CheckHostnameResult> checkHostnames(String wikiId, List<String> hostnames) {
        if (hostnames.isEmpty()) {
            return List.of();
        }

        String hostnamesQuery = hostnames.stream()
                .map(hostname -> "SELECT '" + hostname + "' AS hostname")
                .collect(Collectors.joining(" UNION ALL "));

        return jdbi.inTransaction(handle -> handle
                .createQuery("""
                        SELECT hostname, hostname IN (
                            SELECT hostname
                            FROM citron_spam__ignored_hostname
                            WHERE wiki_id = :wikiId
                        ) AS existed
                        FROM (
                            <hostnamesQuery>
                        ) AS input_hostnames
                        """)
                .bind("wikiId", wikiId)
                .define("hostnamesQuery", hostnamesQuery)
                .mapTo(CheckHostnameResult.class)
                .list());
    }

}
