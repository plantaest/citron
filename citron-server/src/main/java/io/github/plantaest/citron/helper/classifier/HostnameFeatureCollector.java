package io.github.plantaest.citron.helper.classifier;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import io.github.plantaest.citron.client.OpenPageRankClient;
import io.github.plantaest.citron.config.CitronConfig;
import io.github.plantaest.citron.dto.OprGetPageRankResponse;
import io.github.plantaest.citron.helper.Helper;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Startup
@Singleton
public class HostnameFeatureCollector {

    @Inject
    CitronConfig citronConfig;

    @RestClient
    OpenPageRankClient openPageRankClient;

    private Map<String, Integer> akaRanks;
    private Map<String, Integer> trancoRanks;
    private Map<String, Integer> majesticMillionRanks;
    private Set<String> cloudflareRadarDomains;

    private Set<String> specialWords;
    private Set<String> commercialTlds;
    private Set<String> entertainmentTlds;
    private Set<String> gamblingTlds;
    private Set<String> suspiciousTlds;

    @PostConstruct
    void init() throws IOException {
        try (var akaRanksReader = CsvReader.builder()
                .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().akaRanksFilePath()));
             var trancoRanksReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().trancoRanksFilePath()));
             var majesticMillionRanksReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().majesticMillionRanksFilePath()));
             var cloudflareRadarDomainsReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().cloudflareRadarDomainsFilePath()));
             var specialWordsReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().specialWordsFilePath()));
             var commercialTldsReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().commercialTldsFilePath()));
             var entertainmentTldsReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().entertainmentTldsFilePath()));
             var gamblingTldsReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().gamblingTldsFilePath()));
             var suspiciousTldsReader = CsvReader.builder()
                     .ofCsvRecord(Helper.readFromClasspath(citronConfig.classifier().suspiciousTldsFilePath()))
        ) {
            akaRanks = createRankMap(akaRanksReader);
            trancoRanks = createRankMap(trancoRanksReader);
            majesticMillionRanks = createRankMap(majesticMillionRanksReader);
            cloudflareRadarDomains = createPlainSet(cloudflareRadarDomainsReader);
            specialWords = createPlainSet(specialWordsReader);
            commercialTlds = createPlainSet(commercialTldsReader);
            entertainmentTlds = createPlainSet(entertainmentTldsReader);
            gamblingTlds = createPlainSet(gamblingTldsReader);
            suspiciousTlds = createPlainSet(suspiciousTldsReader);
        }
    }

    @CacheResult(cacheName = "hostname-feature-cache")
    public HostnameFeature collect(String hostname) {
        String tld = Helper.getTLD(hostname);
        String topDomain = Helper.getTopDomain(hostname);
        String topPrivateDomain = Helper.getTopPrivateDomain(hostname);
        double openPageRank;

        try {
            OprGetPageRankResponse response = openPageRankClient.getPageRank(List.of(hostname));
            double rank = response.response().getFirst().pageRankDecimal();
            openPageRank = (rank == 0.0) ? -1 : rank;
        } catch (Exception e) {
            Log.errorf("Unable to get OpenPageRank for hostname: %s", hostname);
            openPageRank = -1;
        }

        return HostnameFeatureBuilder.builder()
                .hostname(hostname)
                .openPageRank(openPageRank)
                .openPageRankAvailable(openPageRank != -1)
                .akaRank(Optional.ofNullable(akaRanks.get(topPrivateDomain)).orElse(-1))
                .akaRankAvailable(akaRanks.containsKey(topPrivateDomain))
                .trancoRank(Optional.ofNullable(trancoRanks.get(topDomain)).orElse(-1))
                .trancoRankAvailable(trancoRanks.containsKey(topDomain))
                .majesticMillionRank(Optional.ofNullable(majesticMillionRanks.get(topPrivateDomain)).orElse(-1))
                .majesticMillionRankAvailable(majesticMillionRanks.containsKey(topPrivateDomain))
                .cloudflareRadarAvailable(cloudflareRadarDomains.contains(topDomain))
                .hasSpecialWord(specialWords.stream().anyMatch(hostname::contains))
                .commercialTld(commercialTlds.contains(tld))
                .entertainmentTld(entertainmentTlds.contains(tld))
                .gamblingTld(gamblingTlds.contains(tld))
                .suspiciousTld(suspiciousTlds.contains(tld))
                .hostnameLength(hostname.length())
                .dotCount(Helper.countDot(hostname))
                .digitCount(Helper.countDigit(hostname))
                .isIpv4(Helper.isIPv4(hostname))
                .isTopDomain(Helper.isTopDomain(hostname))
                .isTopPrivateDomain(Helper.isTopPrivateDomain(hostname))
                .build();
    }

    public List<HostnameFeature> collect(List<String> hostnames) {
        return hostnames.stream().map(this::collect).toList();
    }

    private Set<String> createPlainSet(CsvReader<CsvRecord> csvReader) {
        return csvReader.stream()
                .map((record) -> record.getField(0))
                .collect(Collectors.toSet());
    }

    private Map<String, Integer> createRankMap(CsvReader<CsvRecord> csvReader) {
        AtomicInteger counter = new AtomicInteger(1);
        return csvReader.stream()
                .map((record) -> record.getField(0))
                .collect(Collectors.toMap(
                        item -> item,
                        item -> counter.getAndIncrement(),
                        (existing, replacement) -> existing
                ));

    }

}
