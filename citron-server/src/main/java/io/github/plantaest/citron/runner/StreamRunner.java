package io.github.plantaest.citron.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidFactory;
import io.github.plantaest.citron.client.WikiActionClientManager;
import io.github.plantaest.citron.client.WikiRestClient;
import io.github.plantaest.citron.client.WikiRestClientManager;
import io.github.plantaest.citron.client.WikimediaStreamsClient;
import io.github.plantaest.citron.config.CitronConfig;
import io.github.plantaest.citron.dto.Change;
import io.github.plantaest.citron.dto.CheckHostnameResult;
import io.github.plantaest.citron.dto.CompareRevisionsResponse;
import io.github.plantaest.citron.dto.UserGroupsResponse;
import io.github.plantaest.citron.dto.WikiRevisionResponse;
import io.github.plantaest.citron.entity.ReportedHostname;
import io.github.plantaest.citron.entity.ReportedHostnameBuilder;
import io.github.plantaest.citron.enumeration.Model;
import io.github.plantaest.citron.helper.DiffComparison;
import io.github.plantaest.citron.helper.Helper;
import io.github.plantaest.citron.helper.IgnoredSuffixes;
import io.github.plantaest.citron.helper.classifier.ClassificationResult;
import io.github.plantaest.citron.helper.classifier.Classifier;
import io.github.plantaest.citron.helper.classifier.HostnameFeature;
import io.github.plantaest.citron.helper.classifier.HostnameFeatureCollector;
import io.github.plantaest.citron.repository.IgnoredHostnameRepository;
import io.github.plantaest.citron.repository.ReportedHostnameRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.SseEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Startup
@Singleton
@IfBuildProperty(name = "citron.dev.enable-stream-runner", stringValue = "true")
public class StreamRunner {

    @RestClient
    WikimediaStreamsClient wikimediaStreamsClient;
    @Inject
    WikiRestClientManager wikiRestClientManager;
    @Inject
    WikiActionClientManager wikiActionClientManager;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    CitronConfig citronConfig;
    @Inject
    HostnameFeatureCollector hostnameFeatureCollector;
    @Inject
    Classifier classifier;
    @Inject
    ManagedExecutor managedExecutor;
    @Inject
    IgnoredHostnameRepository ignoredHostnameRepository;
    @Inject
    ReportedHostnameRepository reportedHostnameRepository;
    @Inject
    TsidFactory tsidFactory;
    @Inject
    IgnoredSuffixes ignoredSuffixes;

    private Cancellable cancellable;
    private final AtomicReference<String> lastEventIdRef = new AtomicReference<>();
    private final AtomicLong eventCounter = new AtomicLong(0);

    @PostConstruct
    void init() {
        cancellable = Multi.createFrom()
                .deferred(() -> wikimediaStreamsClient.getRawRecentChanges(lastEventIdRef.get()))
                .onSubscription()
                .invoke(() -> Log.info("Connected to Wikimedia EventStreams"))
                .onFailure()
                .retry().withBackOff(Duration.ofSeconds(1), Duration.ofMinutes(2)).indefinitely()
                .subscribe()
                .with(
                        item -> Uni.createFrom().item(item)
                                .onItem().invoke(this::onItem)
                                .subscribe().asCompletionStage(),
                        failure -> Log.errorf("Error on EventStreams: %s", failure),
                        () -> Log.info("EventStreams closed")
                );
    }

    @PreDestroy
    void cleanup() {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    @Scheduled(every = "5m", delay = 5)
    void checkEventFlow() {
        long count = eventCounter.getAndSet(0);
        if (count == 0) {
            Log.warn("No events received in the last 5 minutes. Checking stream...");
            cleanup();
            init();
        } else {
            Log.infof("Received %d events in the last 5 minutes", count);
        }
    }

    private void onItem(SseEvent<String> event) {
        lastEventIdRef.set(event.id());
        eventCounter.incrementAndGet();
        Change change = parse(event.data());
        Set<String> allowedWikiIds = citronConfig.spamModule().wikis().keySet();

        if (change != null
                && !"canary".equals(change.meta().domain())
                && List.of("edit", "new").contains(change.type())
                && !change.bot()
                && !change.patrolled()
                && allowedWikiIds.contains(change.wiki())
                && isWithinLast5Minutes(change.timestamp())
        ) {
            managedExecutor.execute(() -> process(change));
        }
    }

    private void process(Change change) {
        Log.infof("Processing Change(wiki=%s, user=%s, page=%s, revision=%s, type=%s)",
                change.wiki(), change.user(), change.title(), change.revision()._new(), change.type());

        try {
            if (isIgnoredUser(change.wiki(), change.serverName(), change.user())) {
                return;
            }

            List<String> extractedHostnames = new ArrayList<>();
            WikiRestClient wikiRestClient = wikiRestClientManager.getClient(change.serverName());

            if (change.revision().old() != null) {
                // Change type is "edit"
                CompareRevisionsResponse comparison = wikiRestClient
                        .compareRevisions(change.revision().old(), change.revision()._new());
                List<DiffComparison> addedDiffComparisons = Helper.extractAddedDiffComparisons(comparison.diff());
                extractedHostnames.addAll(Helper.extractHostnames(addedDiffComparisons));
            } else {
                // Change type is "new"
                WikiRevisionResponse revision = wikiRestClient.getRevision(change.revision()._new());
                extractedHostnames.addAll(Helper.extractHostnamesFromText(revision.source()));
            }

            if (extractedHostnames.isEmpty()) {
                return;
            }

            List<String> filteredHostnames = extractedHostnames.stream()
                    .filter(Predicate.not(ignoredSuffixes::contains))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            hostnames -> ignoredHostnameRepository.checkHostnames(change.wiki(), hostnames)
                    ))
                    .stream()
                    .filter(Predicate.not(CheckHostnameResult::existed))
                    .map(CheckHostnameResult::hostname)
                    .toList();

            if (filteredHostnames.isEmpty()) {
                return;
            }

            List<HostnameFeature> hostnameFeatures = filteredHostnames.stream()
                    .map(hostnameFeatureCollector::collect)
                    .toList();
            List<ClassificationResult> classificationResults = classifier
                    .classify(hostnameFeatures, Model.getDefaultModel());

            Instant now = Instant.now();
            List<ReportedHostname> reportedHostnames = classificationResults.stream()
                    .map(classificationResult -> ReportedHostnameBuilder.builder()
                            .id(tsidFactory.create().toLong())
                            .createdAt(now)
                            .wikiId(change.wiki())
                            .user(change.user())
                            .page(change.title())
                            .revisionId(change.revision()._new())
                            .revisionTimestamp(change.timestamp())
                            .hostname(classificationResult.hostname())
                            .score(BigDecimal.valueOf(classificationResult.probability())
                                    .setScale(6, RoundingMode.HALF_UP).doubleValue())
                            .modelNumber(Model.getDefaultModel().getNumber())
                            .build())
                    .toList();

            for (var reportedHostname : reportedHostnames) {
                reportedHostnameRepository.save(reportedHostname);
            }

            Log.infof("Processed Change(wiki=%s, user=%s, page=%s, revision=%s, type=%s): Extracted: (%s) %s; Reported: (%s) %s",
                    change.wiki(), change.user(), change.title(), change.revision()._new(), change.type(),
                    extractedHostnames.size(), extractedHostnames,
                    reportedHostnames.size(), reportedHostnames.stream()
                            .map(hostname -> "%s (%s)".formatted(hostname.hostname(), hostname.score()))
                            .toList());
        } catch (Exception e) {
            Log.errorf("Unable to process Change(wiki=%s, user=%s, page=%s, revision=%s, type=%s): %s",
                    change.wiki(), change.user(), change.title(), change.revision()._new(), change.type(), e.getMessage());
        }
    }

    private Change parse(String json) {
        try {
            return objectMapper.readValue(json, Change.class);
        } catch (Exception e) {
            Log.errorf("Failed to parse the change: %s", e.getMessage());
            return null;
        }
    }

    @CacheResult(cacheName = "user-groups-cache")
    protected UserGroupsResponse getCachedUserGroups(String wikiServerName, String username) {
        return wikiActionClientManager.getClient(wikiServerName).getUserGroups(username);
    }

    private boolean isIgnoredUser(String wikiId, String wikiServerName, String username) {
        if (Helper.isIP(username)) {
            return false;
        }

        List<String> ignoredUserGroups = citronConfig.spamModule().wikis().get(wikiId).ignoredUserGroups();

        try {
            var userGroupsResponse = getCachedUserGroups(wikiServerName, username);
            List<String> userGroups = userGroupsResponse.query().users().getFirst().groups();
            return ignoredUserGroups.stream().anyMatch(userGroups::contains);
        } catch (Exception e) {
            Log.errorf("Unable to retrieve user groups for %s on %s: %s", username, wikiId, e.getMessage());
            return false;
        }
    }

    private boolean isWithinLast5Minutes(long timestamp) {
        long currentTimestamp = Instant.now().getEpochSecond();
        return currentTimestamp - timestamp <= 5 * 60 && currentTimestamp >= timestamp;
    }

}
