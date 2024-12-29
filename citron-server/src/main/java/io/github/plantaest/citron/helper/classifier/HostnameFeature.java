package io.github.plantaest.citron.helper.classifier;

import io.github.plantaest.citron.config.recordbuilder.Builder;

@Builder
public record HostnameFeature(
        String hostname,
        double openPageRank,
        boolean openPageRankAvailable,
        int akaRank,
        boolean akaRankAvailable,
        int trancoRank,
        boolean trancoRankAvailable,
        int majesticMillionRank,
        boolean majesticMillionRankAvailable,
        boolean cloudflareRadarAvailable,
        boolean hasSpecialWord,
        boolean commercialTld,
        boolean entertainmentTld,
        boolean gamblingTld,
        boolean suspiciousTld,
        int hostnameLength,
        int dotCount,
        int digitCount,
        boolean isIpv4,
        boolean isTopDomain,
        boolean isTopPrivateDomain
) {}
