package io.github.plantaest.citron.repository;

import io.github.plantaest.citron.entity.Feedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;

@ApplicationScoped
public class FeedbackRepository {

    @Inject
    Jdbi jdbi;

    public void save(Feedback feedback) {
        jdbi.useTransaction(handle -> handle
                .createUpdate("""
                        INSERT INTO citron_spam__feedback (
                            id, created_at, created_by,
                            wiki_id, report_date,
                            hostname, status, hash
                        ) VALUES (
                            :id, :createdAt, :createdBy,
                            :wikiId, :reportDate,
                            :hostname, :status, :hash
                        )
                        """)
                .bindMethods(feedback)
                .execute());
    }

}
