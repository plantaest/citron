package io.github.plantaest.citron.repository;

import io.github.plantaest.citron.entity.ReportedHostname;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@ApplicationScoped
public class ReportedHostnameRepository {

    @Inject
    Jdbi jdbi;

    public void save(ReportedHostname reportedHostname) {
        jdbi.useTransaction(handle -> handle
                .createUpdate("""
                        INSERT INTO citron_spam__reported_hostname (
                            id, created_at,
                            wiki_id, user, page,
                            revision_id, revision_timestamp,
                            hostname, score, model_number
                        ) VALUES (
                            :id, :createdAt,
                            :wikiId, :user, :page,
                            :revisionId, :revisionTimestamp,
                            :hostname, :score, :modelNumber
                        )
                        """)
                .bindMethods(reportedHostname)
                .execute());
    }

    public List<ReportedHostname> findAllForToday(String wikiId) {
        return jdbi.inTransaction(handle -> handle
                .createQuery("""
                        SELECT *
                        FROM citron_spam__reported_hostname
                        WHERE wiki_id = :wikiId
                            AND created_at >= CONVERT_TZ(CURDATE(), '+00:00', :timeZone)
                            AND created_at <= CONVERT_TZ(NOW(), '+00:00', :timeZone)
                        """)
                .bind("wikiId", wikiId)
                // TODO: Improve timeZone
                .bind("timeZone", ConfigUtils.isProfileActive("prod")
                        ? "+00:00"
                        : ZonedDateTime.now(ZoneId.systemDefault()).getOffset().toString())
                .mapTo(ReportedHostname.class)
                .list());
    }

    public boolean hasRecordsForToday(String wikiId) {
        return jdbi.inTransaction(handle -> handle
                .createQuery("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM citron_spam__reported_hostname
                            WHERE wiki_id = :wikiId
                                AND created_at >= CONVERT_TZ(CURDATE(), '+00:00', :timeZone)
                                AND created_at <= CONVERT_TZ(NOW(), '+00:00', :timeZone)
                        ) AS has_result;
                        """)
                .bind("wikiId", wikiId)
                .bind("timeZone", ConfigUtils.isProfileActive("prod")
                        ? "+00:00"
                        : ZonedDateTime.now(ZoneId.systemDefault()).getOffset().toString())
                .mapTo(boolean.class)
                .one());
    }

}
