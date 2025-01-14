package io.github.plantaest.citron.config;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Map;

@ConfigMapping(prefix = "citron")
public interface CitronConfig {
    String version();

    int reportVersion();

    Bot bot();

    interface Bot {
        String username();

        String password();
    }

    List<Model> models();

    interface Model {
        String id();

        String path();
    }

    Classifier classifier();

    interface Classifier {
        String akaRanksFilePath();

        String trancoRanksFilePath();

        String majesticMillionRanksFilePath();

        String cloudflareRadarDomainsFilePath();

        String specialWordsFilePath();

        String commercialTldsFilePath();

        String entertainmentTldsFilePath();

        String gamblingTldsFilePath();

        String suspiciousTldsFilePath();

        String openPageRankApiKey();
    }

    String ignoredSuffixesFilePath();

    SpamModule spamModule();

    interface SpamModule {
        Map<String, Wiki> wikis();

        interface Wiki {
            String wikiId();

            String wikiServerName();

            List<String> ignoredUserGroups();

            String announcementPage();

            String announcementSection();
        }
    }

    Dev dev();

    interface Dev {
        boolean enableStreamRunner();

        boolean enableReportRunner();

        boolean enableFeedbackRunner();
    }
}
