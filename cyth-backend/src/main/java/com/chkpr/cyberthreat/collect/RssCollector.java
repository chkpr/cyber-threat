package com.chkpr.cyberthreat.collect;

import com.chkpr.cyberthreat.config.RssProperties;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a configurable list of RSS/Atom feeds (see app.rss.feeds).
 * Each configured feed becomes a batch of items tagged with its source name.
 */
@Component
public class RssCollector implements Collector {

    private static final Logger log = LoggerFactory.getLogger(RssCollector.class);

    private final RssProperties properties;

    public RssCollector(RssProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "RSS";
    }

    @Override
    public List<RawItem> collect() {
        List<RawItem> items = new ArrayList<>();
        for (RssProperties.Feed feed : properties.getFeeds()) {
            try {
                items.addAll(readFeed(feed));
            } catch (Exception e) {
                log.warn("RSS feed '{}' failed: {}", feed.getName(), e.getMessage());
            }
        }
        log.info("RSS: {} entries fetched from {} feed(s)", items.size(), properties.getFeeds().size());
        return items;
    }

    private List<RawItem> readFeed(RssProperties.Feed feed) throws Exception {
        List<RawItem> items = new ArrayList<>();
        URI uri = URI.create(feed.getUrl());

        try (XmlReader reader = new XmlReader(uri.toURL())) {
            SyndFeed syndFeed = new SyndFeedInput().build(reader);

            for (SyndEntry entry : syndFeed.getEntries()) {
                String link = entry.getLink();
                if (link == null || link.isBlank()) {
                    continue;
                }

                Instant publishedAt = null;
                if (entry.getPublishedDate() != null) {
                    publishedAt = entry.getPublishedDate().toInstant();
                } else if (entry.getUpdatedDate() != null) {
                    publishedAt = entry.getUpdatedDate().toInstant();
                }

                String description = entry.getDescription() != null
                        ? entry.getDescription().getValue()
                        : null;

                List<String> tags = new ArrayList<>();
                tags.add("rss");

                items.add(new RawItem(
                        feed.getName(),
                        link,                 // l'URL sert d'identifiant unique pour la dédup
                        entry.getTitle(),
                        link,
                        description,
                        publishedAt,
                        false,
                        null,
                        tags
                ));
            }
        }
        return items;
    }
}