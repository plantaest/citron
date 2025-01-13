package io.github.plantaest.citron.helper;

import com.google.common.net.InternetDomainName;
import io.github.plantaest.citron.dto.CompareRevisionsResponse.Diff;
import io.github.plantaest.citron.dto.CompareRevisionsResponse.Diff.HighlightRange;
import io.quarkus.logging.Log;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}$"
    );

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            // Ref: https://stackoverflow.com/a/163398
            "\\bhttps?://[-a-zA-Z0-9+&@#/%?=~_!:,.;]*[-a-zA-Z0-9+&@#/%=~_]\\b",
            Pattern.MULTILINE
    );

    // Common helper function

    public static Reader readFromClasspath(final String filePath) throws FileNotFoundException {
        final var inputStream = Helper.class.getClassLoader().getResourceAsStream(filePath);

        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found on classpath: " + filePath);
        }

        return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }

    // Helper function for classifier

    public static boolean isIPv4(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            return false;
        }
        return IPV4_PATTERN.matcher(hostname).matches();
    }

    public static int countDot(String hostname) {
        return (int) hostname.chars().filter(ch -> ch == '.').count();
    }

    public static int countDigit(String hostname) {
        return (int) hostname.chars().filter(Character::isDigit).count();
    }

    public static String getTLD(String hostname) {
        try {
            var registrySuffix = InternetDomainName.from(hostname).registrySuffix();
            if (registrySuffix != null) {
                return registrySuffix.toString();
            } else {
                return "__NULL__";
            }
        } catch (Exception e) {
            return "__ERROR__";
        }
    }

    public static String getTopDomain(String hostname) {
        try {
            return InternetDomainName.from(hostname).topDomainUnderRegistrySuffix().toString();
        } catch (Exception e) {
            return "__ERROR__";
        }
    }

    public static String getTopPrivateDomain(String hostname) {
        try {
            return InternetDomainName.from(hostname).topPrivateDomain().toString();
        } catch (Exception e) {
            return "__ERROR__";
        }
    }

    public static boolean isTopDomain(String hostname) {
        try {
            return InternetDomainName.from(hostname).isTopDomainUnderRegistrySuffix();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isTopPrivateDomain(String hostname) {
        try {
            return InternetDomainName.from(hostname).isTopPrivateDomain();
        } catch (Exception e) {
            return false;
        }
    }

    // Helper function for stream runner

    public static boolean isIP(String str) {
        try {
            InetAddress address = InetAddress.getByName(str);
            return address instanceof Inet4Address || address instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static List<DiffComparison> extractAddedDiffComparisons(List<Diff> diffs) {
        List<DiffComparison> diffComparisons = new ArrayList<>();

        for (var diff : diffs) {
            if (diff.text().isBlank()) {
                continue;
            }

            if (diff.type() == 1) {
                diffComparisons.add(new DiffComparison(null, diff.text()));
                continue;
            }

            var highlightRanges = Optional.ofNullable(diff.highlightRanges()).orElse(List.of());

            if (highlightRanges.isEmpty()) {
                continue;
            }

            if (diff.type() == 3 || diff.type() == 5) {
                boolean hasTypeZero = highlightRanges.stream().anyMatch(range -> range.type() == 0);
                boolean allTypeZero = highlightRanges.stream().allMatch(range -> range.type() == 0);
                List<HighlightRange> additionHighlightRanges = highlightRanges.stream()
                        .filter(range -> range.type() == 0)
                        .toList();
                List<HighlightRange> deletionHighlightRanges = highlightRanges.stream()
                        .filter(range -> range.type() == 1)
                        .toList();

                if (hasTypeZero) {
                    if (allTypeZero) {
                        diffComparisons.add(new DiffComparison(
                                eliminateDiffTextByHighlightRanges(diff.text(), additionHighlightRanges),
                                diff.text()
                        ));
                    } else {
                        diffComparisons.add(new DiffComparison(
                                eliminateDiffTextByHighlightRanges(diff.text(), additionHighlightRanges),
                                eliminateDiffTextByHighlightRanges(diff.text(), deletionHighlightRanges)
                        ));
                    }
                }
            }
        }

        return diffComparisons;
    }

    private static String eliminateDiffTextByHighlightRanges(String text, List<HighlightRange> highlightRanges) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        List<Byte> bytes = new ArrayList<>();

        for (var range : highlightRanges) {
            for (int i = range.start(); i < range.start() + range.length(); i++) {
                textBytes[i] = 0;
            }
        }

        for (byte b : textBytes) {
            if (b != 0) {
                bytes.add(b);
            }
        }

        byte[] resultBytes = new byte[bytes.size()];

        for (int i = 0; i < resultBytes.length; i++) {
            resultBytes[i] = bytes.get(i);
        }

        return new String(resultBytes, StandardCharsets.UTF_8);
    }

    public static List<String> extractHostnames(List<DiffComparison> addedDiffComparisons) {
        Set<String> extractedHostnames = new TreeSet<>();

        for (DiffComparison diffComparison : addedDiffComparisons) {
            if (diffComparison.newText() != null) {
                if (diffComparison.oldText() == null) {
                    extractedHostnames.addAll(extractHostnamesFromText(diffComparison.newText()));
                } else {
                    Set<String> hostnamesFromOldText = extractHostnamesFromText(diffComparison.oldText());
                    Set<String> hostnamesFromNewText = extractHostnamesFromText(diffComparison.newText());
                    extractedHostnames.addAll(hostnamesFromNewText.stream()
                            .filter(Predicate.not(hostnamesFromOldText::contains))
                            .toList());
                }
            }
        }

        return extractedHostnames.stream().toList();
    }

    private static Set<String> extractHostnamesFromText(String text) {
        Set<String> extractedHostnames = new TreeSet<>();

        if (text.contains("http://") || text.contains("https://")) {
            Matcher matcher = HOSTNAME_PATTERN.matcher(text);
            while (matcher.find()) {
                String link = matcher.group();
                try {
                    URI uri = new URI(link);
                    String hostname = uri.getHost();
                    if (hostname != null && hostname.contains(".")) {
                        String[] parts = hostname.split("\\.");
                        if (parts.length >= 2 && parts[parts.length - 1].length() >= 2) {
                            extractedHostnames.add(hostname.toLowerCase());
                        }
                    }
                } catch (URISyntaxException e) {
                    Log.errorf("Unable to parse URI: %s", e);
                }
            }
        }

        return extractedHostnames;
    }

}
