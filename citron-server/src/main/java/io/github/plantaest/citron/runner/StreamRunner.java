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
import io.github.plantaest.citron.entity.ReportedHostname;
import io.github.plantaest.citron.entity.ReportedHostnameBuilder;
import io.github.plantaest.citron.enumeration.Model;
import io.github.plantaest.citron.helper.DiffComparison;
import io.github.plantaest.citron.helper.Helper;
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
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.SseEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Startup
@ApplicationScoped
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

    private Cancellable cancellable;
    private final AtomicReference<String> lastEventIdRef = new AtomicReference<>();

    @PostConstruct
    void init() {
        cancellable = Multi.createFrom()
                .deferred(() -> wikimediaStreamsClient.getRawRecentChanges(lastEventIdRef.get()))
                .onSubscription()
                .invoke(() -> Log.info("Connected to Wikimedia EventStreams"))
                .onFailure()
                .retry().indefinitely()
                .subscribe()
                .with(
                        this::onItem,
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

    private void onItem(SseEvent<String> event) {
        lastEventIdRef.set(event.id());
        Change change = parse(event.data());
        Set<String> allowedWikiIds = citronConfig.spamModule().wikis().keySet();

        if (change != null
                && !"canary".equals(change.meta().domain())
                && "edit".equals(change.type())
                && !change.bot()
                && allowedWikiIds.contains(change.wiki())
        ) {
            managedExecutor.execute(() -> process(change));
        }
    }

    private void process(Change change) {
        Log.infof("Processing Change(wiki=%s, user=%s, page=%s, revision=%s)",
                change.wiki(), change.user(), change.title(), change.revision()._new());

        try {
            if (isIgnoredUser(change.wiki(), change.serverName(), change.user())) {
                return;
            }

            WikiRestClient wikiRestClient = wikiRestClientManager.getClient(change.serverName());
            CompareRevisionsResponse comparison = wikiRestClient
                    .compareRevisions(change.revision().old(), change.revision()._new());
            List<DiffComparison> addedDiffComparisons = Helper.extractAddedDiffComparisons(comparison.diff());
            List<String> extractedHostnames = Helper.extractHostnames(addedDiffComparisons);

            if (extractedHostnames.isEmpty()) {
                return;
            }

            List<String> filteredHostnames = ignoredHostnameRepository
                    .checkHostnames(change.wiki(), extractedHostnames)
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

            Log.infof("Processed Change(wiki=%s, user=%s, page=%s, revision=%s): Extracted: (%s) %s; Reported: (%s) %s",
                    change.wiki(), change.user(), change.title(), change.revision()._new(),
                    extractedHostnames.size(), extractedHostnames,
                    reportedHostnames.size(), reportedHostnames.stream()
                            .map(hostname -> "%s (%s)".formatted(hostname.hostname(), hostname.score()))
                            .toList());
        } catch (Exception e) {
            Log.errorf("Unable to process Change(wiki=%s, user=%s, page=%s, revision=%s): %s",
                    change.wiki(), change.user(), change.title(), change.revision()._new(), e.getMessage());
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

}
