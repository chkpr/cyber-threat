package com.chkpr.cyberthreat.collect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the CISA Known Exploited Vulnerabilities (KEV) catalog.
 * Every entry is, by definition, actively exploited -> flagged inKev = true,
 * which the scoring service routes to ALERT.
 */
@Component
public class CisaKevCollector implements Collector {

    private static final Logger log = LoggerFactory.getLogger(CisaKevCollector.class);
    private static final String FEED_URL =
            "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json";

    private final RestClient restClient;
    
    @Value("${app.kev.max-age-days:14}")
    private int maxAgeDays;

    public CisaKevCollector(RestClient restClient) {
        this.restClient = restClient;
    }
    


    @Override
    public String name() {
        return "CISA KEV";
    }

    @Override
    public List<RawItem> collect() {
        KevFeed feed = restClient.get()
                .uri(FEED_URL)
                .retrieve()
                .body(KevFeed.class);

        if (feed == null || feed.vulnerabilities() == null) {
            log.warn("CISA KEV feed returned no data");
            return List.of();
        }

        List<RawItem> items = new ArrayList<>();
        for (KevVuln v : feed.vulnerabilities()) {
            Instant publishedAt = null;
            if (v.dateAdded() != null && !v.dateAdded().isBlank()) {
                publishedAt = LocalDate.parse(v.dateAdded())
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();
            }
            
            if (publishedAt != null && maxAgeDays > 0) {
                Instant cutoff = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);
                if (publishedAt.isBefore(cutoff)) {
                    continue; // entrée KEV trop ancienne pour une veille quotidienne
                }
            }
            
            List<String> tags = new ArrayList<>();
            tags.add("cve");
            if (v.vendorProject() != null) {
                tags.add(v.vendorProject().toLowerCase());
            }
            if ("Known".equalsIgnoreCase(v.knownRansomwareCampaignUse())) {
                tags.add("ransomware");
            }

            items.add(new RawItem(
                    name(),
                    v.cveID(),
                    v.vulnerabilityName() != null ? v.vulnerabilityName() : v.cveID(),
                    "https://nvd.nist.gov/vuln/detail/" + v.cveID(),
                    v.shortDescription(),
                    publishedAt,
                    true,
                    null,
                    tags
            ));
        }
        log.info("CISA KEV: {} vulnerabilities fetched", items.size());
        return items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KevFeed(List<KevVuln> vulnerabilities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KevVuln(
            @JsonProperty("cveID") String cveID,
            String vendorProject,
            String product,
            String vulnerabilityName,
            String dateAdded,
            String shortDescription,
            String knownRansomwareCampaignUse
    ) {
    }
    
    private boolean isTooOld(Instant publishedAt) {
        if (publishedAt == null) {
            return true; // pas de date -> on ne peut pas garantir la récidence, on exclut
        }
        Instant cutoff = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);
        return publishedAt.isBefore(cutoff);
    }
}
