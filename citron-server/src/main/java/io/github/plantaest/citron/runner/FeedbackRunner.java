package io.github.plantaest.citron.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidFactory;
import io.github.plantaest.citron.client.WikiActionClient;
import io.github.plantaest.citron.client.WikiActionClientManager;
import io.github.plantaest.citron.client.WikiRestClient;
import io.github.plantaest.citron.client.WikiRestClientManager;
import io.github.plantaest.citron.config.CitronConfig;
import io.github.plantaest.citron.dto.Report;
import io.github.plantaest.citron.dto.ReportBuilder;
import io.github.plantaest.citron.dto.ReportFeedbackBuilder;
import io.github.plantaest.citron.dto.WikiPageResponse;
import io.github.plantaest.citron.entity.Feedback;
import io.github.plantaest.citron.entity.FeedbackBuilder;
import io.github.plantaest.citron.repository.FeedbackRepository;
import io.github.plantaest.citron.repository.IgnoredHostnameRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@IfBuildProperty(name = "citron.dev.enable-feedback-runner", stringValue = "true")
public class FeedbackRunner {

    @Inject
    CitronConfig citronConfig;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    WikiRestClientManager wikiRestClientManager;
    @Inject
    WikiActionClientManager wikiActionClientManager;
    @Inject
    FeedbackRepository feedbackRepository;
    @Inject
    IgnoredHostnameRepository ignoredHostnameRepository;
    @Inject
    TsidFactory tsidFactory;

    @Scheduled(cron = "5 0 0 * * ?", timeZone = "UTC")
    public void sync() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime yesterday = now.minusDays(1);
        var wikis = citronConfig.spamModule().wikis().values();

        try {
            for (var wiki : wikis) {
                WikiRestClient wikiRestClient = wikiRestClientManager.getClient(wiki.wikiServerName());
                WikiActionClient wikiActionClient = wikiActionClientManager.getClient(wiki.wikiServerName());
                String reportPageTitle = "Project:Citron/Spam/%s.json".formatted(yesterday.toLocalDate());

                Report yesterdayReport = null;

                try {
                    WikiPageResponse wikiPageResponse = wikiRestClient.getPage(reportPageTitle);
                    yesterdayReport = objectMapper.readValue(wikiPageResponse.source(), Report.class);
                } catch (Exception e) {
                    Log.warnf("Unable to get or parse report from '%s' on %s; it may not exist yet: %s",
                            reportPageTitle, wiki.wikiId(), e.getMessage());
                }

                if (yesterdayReport == null || yesterdayReport.feedbacks().isEmpty()) {
                    continue;
                }

                // Save feedbacks to DB
                for (Report.Feedback reportFeedback : yesterdayReport.feedbacks()) {
                    Feedback feedback = FeedbackBuilder.builder()
                            .id(tsidFactory.create().toLong())
                            .createdAt(Instant.parse(reportFeedback.createdAt()))
                            .createdBy(reportFeedback.createdBy())
                            .wikiId(wiki.wikiId())
                            .reportDate(yesterday.toLocalDate().toString())
                            .hostname(reportFeedback.hostname())
                            .status(reportFeedback.status())
                            .hash(reportFeedback.hash())
                            .build();
                    feedbackRepository.save(feedback);
                }

                // Save ignored hostnames to DB
                Set<String> ignoredHostnames = yesterdayReport.feedbacks().stream()
                        .collect(Collectors.groupingBy(
                                Report.Feedback::hostname,
                                Collectors.mapping(Report.Feedback::status, Collectors.toSet())
                        ))
                        .entrySet()
                        .stream()
                        .filter(entry -> {
                            Set<Integer> statuses = entry.getValue();
                            return statuses.contains(0) && !statuses.contains(1);
                        })
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());

                for (String ignoredHostname : ignoredHostnames) {
                    if (!ignoredHostnameRepository.exists(wiki.wikiId(), ignoredHostname)) {
                        ignoredHostnameRepository.save(wiki.wikiId(), ignoredHostname);
                    }
                }

                // Change value of "synced" property and save on wiki
                String updatedAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                Report report = ReportBuilder.builder(yesterdayReport)
                        .updatedAt(updatedAt)
                        .feedbacks(yesterdayReport.feedbacks().stream()
                                .map(feedback -> ReportFeedbackBuilder.builder(feedback)
                                        .synced(true)
                                        .build())
                                .toList())
                        .build();

                wikiActionClient.edit(Map.of(
                        "title", reportPageTitle,
                        "text", objectMapper.writeValueAsString(report),
                        "summary", "Sync feedback of Citron/Spam report",
                        "bot", "true",
                        "contentmodel", "json"
                ));

                Log.infof("Synced feedback of report '%s' on %s", yesterday.toLocalDate(), wiki.wikiId());
            }
        } catch (Exception e) {
            Log.errorf("Unable to sync feedback of report '%s': %s", now.toLocalDate(), e.getMessage());
        }
    }

}
