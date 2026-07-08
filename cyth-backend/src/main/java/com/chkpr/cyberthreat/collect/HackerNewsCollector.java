package com.chkpr.cyberthreat.collect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Collects top Hacker News stories via the public Firebase API.
 * Keeps only stories whose score clears a threshold, then (optionally)
 * those matching security keywords so the digest stays on-topic.
 */
@Component
public class HackerNewsCollector implements Collector {

    private static final Logger log = LoggerFactory.getLogger(HackerNewsCollector.class);
    private static final String TOP_STORIES = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM = "https://hacker-news.firebaseio.com/v0/item/{id}.json";

    private static final List<String> SECURITY_KEYWORDS = List.of(
            "security", "vulnerability", "cve", "exploit", "breach", "malware",
            "ransomware", "phishing", "zero-day", "0-day", "rce", "backdoor",
            "attack", "hacked", "leak", "authentication", "encryption", "cyber"
    );

    private final RestClient restClient;

    @Value("${app.hackernews.top-count:50}")
    private int topCount;

    @Value("${app.hackernews.min-score:50}")
    private int minScore;

    @Value("${app.hackernews.security-only:true}")
    private boolean securityOnly;

    public HackerNewsCollector(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String name() {
        return "Hacker News";
    }

    @Override
    public List<RawItem> collect() {
        Long[] ids = restClient.get().uri(TOP_STORIES).retrieve().body(Long[].class);
        if (ids == null) {
            log.warn("Hacker News returned no story ids");
            return List.of();
        }

        List<RawItem> items = new ArrayList<>();
        int limit = Math.min(topCount, ids.length);
        for (int i = 0; i < limit; i++) {
            HnItem story = restClient.get().uri(ITEM, ids[i]).retrieve().body(HnItem.class);
            if (story == null || !"story".equals(story.type()) || story.url() == null) {
                continue;
            }
            if (story.score() < minScore) {
                continue;
            }
            if (securityOnly && !matchesSecurity(story.title())) {
                continue;
            }

            Instant publishedAt = story.time() != null ? Instant.ofEpochSecond(story.time()) : null;
            items.add(new RawItem(
                    name(),
                    String.valueOf(story.id()),
                    story.title(),
                    story.url(),
                    null,
                    publishedAt,
                    false,
                    null,
                    new ArrayList<>(List.of("hn"))
            ));
        }
        log.info("Hacker News: {} stories retained", items.size());
        return items;
    }

    private boolean matchesSecurity(String title) {
        if (title == null) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        return SECURITY_KEYWORDS.stream().anyMatch(lower::contains);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HnItem(Long id, String title, String url, Integer score, String by, Long time, String type) {
    }
}
