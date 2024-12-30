package io.github.plantaest.citron.repository;

import io.github.plantaest.citron.entity.ReportedHostname;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;

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

    public List<ReportedHostname> findAllByDateRange(String wikiId, ZonedDateTime from, ZonedDateTime to) {
        return jdbi.inTransaction(handle -> handle
                .createQuery("""
                        SELECT *
                        FROM citron_spam__reported_hostname
                        WHERE wiki_id = :wikiId
                            AND created_at >= :from
                            AND created_at <= :to
                        """)
                .bind("wikiId", wikiId)
                .bind("from", from)
                .bind("to", to)
                .mapTo(ReportedHostname.class)
                .list());
    }

    public boolean hasRecordsByDateRange(String wikiId, ZonedDateTime from, ZonedDateTime to) {
        return jdbi.inTransaction(handle -> handle
                .createQuery("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM citron_spam__reported_hostname
                            WHERE wiki_id = :wikiId
                                AND created_at >= :from
                                AND created_at <= :to
                        ) AS has_result;
                        """)
                .bind("wikiId", wikiId)
                .bind("from", from)
                .bind("to", to)
                .mapTo(boolean.class)
                .one());
    }

}
