package io.github.plantaest.citron.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.plantaest.citron.client.WikiActionClient;
import io.github.plantaest.citron.client.WikiActionClientManager;
import io.github.plantaest.citron.client.WikiRestClient;
import io.github.plantaest.citron.client.WikiRestClientManager;
import io.github.plantaest.citron.config.CitronConfig;
import io.github.plantaest.citron.dto.Report;
import io.github.plantaest.citron.dto.ReportBuilder;
import io.github.plantaest.citron.dto.ReportHostnameBuilder;
import io.github.plantaest.citron.dto.ReportRevisionBuilder;
import io.github.plantaest.citron.dto.WikiPageResponse;
import io.github.plantaest.citron.entity.ReportedHostname;
import io.github.plantaest.citron.repository.ReportedHostnameRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@IfBuildProperty(name = "citron.dev.enable-report-runner", stringValue = "true")
public class ReportRunner {

    @Inject
    CitronConfig citronConfig;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    WikiRestClientManager wikiRestClientManager;
    @Inject
    WikiActionClientManager wikiActionClientManager;
    @Inject
    ReportedHostnameRepository reportedHostnameRepository;

    @Scheduled(cron = "59 59 * * * ?", timeZone = "UTC")
    public void report() throws JsonProcessingException {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startOfDay = now.with(LocalTime.MIN);
        var wikis = citronConfig.spamModule().wikis().values();

        for (var wiki : wikis) {
            List<ReportedHostname> reportedHostnames = reportedHostnameRepository
                    .findAllByDateRange(wiki.wikiId(), startOfDay, now);

            if (reportedHostnames.isEmpty()) {
                continue;
            }

            WikiRestClient wikiRestClient = wikiRestClientManager.getClient(wiki.wikiServerName());
            WikiActionClient wikiActionClient = wikiActionClientManager.getClient(wiki.wikiServerName());
            String reportPageTitle = "Project:Citron/Spam/%s.json".formatted(now.toLocalDate());

            List<Report.Feedback> feedbacks = new ArrayList<>();

            try {
                WikiPageResponse wikiPageResponse = wikiRestClient.getPage(reportPageTitle);
                Report currentReport = objectMapper.readValue(wikiPageResponse.source(), Report.class);
                feedbacks.addAll(currentReport.feedbacks());
            } catch (Exception e) {
                Log.warnf("Unable to get or parse report from '%s' on %s; it may not exist yet: %s",
                        reportPageTitle, wiki.wikiId(), e.getMessage());
            }

            List<Report.Hostname> hostnames = reportedHostnames.stream()
                    .collect(Collectors.groupingBy(ReportedHostname::hostname))
                    .entrySet().stream()
                    .map(entry -> {
                        String hostname = entry.getKey();
                        List<ReportedHostname> relatedReportedHostnames = entry.getValue();

                        String time = relatedReportedHostnames.stream()
                                .map(ReportedHostname::revisionTimestamp)
                                .min(Long::compare)
                                .map(timestamp -> Instant.ofEpochSecond(timestamp)
                                        .atOffset(ZoneOffset.UTC)
                                        .format(DateTimeFormatter.ofPattern("HH:mm")))
                                .orElse("__:__");

                        double averageScore = relatedReportedHostnames.stream()
                                .mapToDouble(ReportedHostname::score)
                                .average()
                                .orElse(-1);

                        List<Long> revisionIds = relatedReportedHostnames.stream()
                                .map(ReportedHostname::revisionId)
                                .toList();

                        return ReportHostnameBuilder.builder()
                                .hostname(hostname)
                                .time(time)
                                .score(averageScore)
                                .revisionIds(revisionIds)
                                .build();
                    })
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .toList();

            Map<Long, Report.Revision> revisions = reportedHostnames.stream()
                    .collect(Collectors.toMap(
                            ReportedHostname::revisionId,
                            rh -> ReportRevisionBuilder.builder()
                                    .page(rh.page())
                                    .user(rh.user())
                                    .build(),
                            (existing, replacement) -> existing
                    ));

            String updatedAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

            Report report = ReportBuilder.builder()
                    .version(citronConfig.reportVersion())
                    .updatedAt(updatedAt)
                    .hostnames(hostnames)
                    .revisions(revisions)
                    .feedbacks(feedbacks)
                    .build();

            // Save report
            wikiActionClient.edit(Map.of(
                    "title", reportPageTitle,
                    "text", objectMapper.writeValueAsString(report),
                    "summary", "Update Citron/Spam report at %s".formatted(updatedAt),
                    "bot", "true",
                    "contentmodel", "json"
            ));

            Log.infof("Updated report '%s' on %s", now.toLocalDate(), wiki.wikiId());
        }
    }

    @Scheduled(cron = "59 59 * * * ?", timeZone = "UTC")
    public void announce() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startOfDay = now.with(LocalTime.MIN);
        var wikis = citronConfig.spamModule().wikis().values();

        for (var wiki : wikis) {
            if (!reportedHostnameRepository.hasRecordsByDateRange(wiki.wikiId(), startOfDay, now)) {
                continue;
            }

            WikiRestClient wikiRestClient = wikiRestClientManager.getClient(wiki.wikiServerName());
            WikiActionClient wikiActionClient = wikiActionClientManager.getClient(wiki.wikiServerName());
            String marker = "{{#invoke:Citron/Spam|report|date=%s}}".formatted(now.toLocalDate());
            String announcementPageContent = null;

            try {
                WikiPageResponse wikiPageResponse = wikiRestClient.getPage(wiki.announcementPage());
                announcementPageContent = wikiPageResponse.source();
            } catch (Exception e) {
                Log.errorf("Unable to retrieve content of page '%s' on %s: %s",
                        wiki.announcementPage(), wiki.wikiId(), e.getMessage());
            }

            if (announcementPageContent == null || announcementPageContent.contains(marker)) {
                continue;
            }

            wikiActionClient.edit(Map.of(
                    "title", wiki.announcementPage(),
                    "text", marker + "\n~~~~",
                    "summary", "Announce Citron/Spam report %s".formatted(now.toLocalDate()),
                    "bot", "true",
                    "section", "new",
                    "sectiontitle", wiki.announcementSection()
                            .replace("{day}", String.valueOf(now.getDayOfMonth()))
                            .replace("{month}", String.valueOf(now.getMonthValue()))
                            .replace("{year}", String.valueOf(now.getYear()))
            ));

            wikiActionClient.purge(wiki.announcementPage());

            Log.infof("Announced report '%s' on %s", now.toLocalDate(), wiki.wikiId());
        }
    }

}
