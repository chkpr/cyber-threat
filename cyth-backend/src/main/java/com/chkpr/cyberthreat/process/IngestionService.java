package com.chkpr.cyberthreat.process;

import com.chkpr.cyberthreat.collect.RawItem;
import com.chkpr.cyberthreat.item.Item;
import com.chkpr.cyberthreat.item.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Turns a RawItem into a persisted, scored Item. Deduplicates on
 * (source, externalId) so re-running a collector never creates duplicates.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ItemRepository itemRepository;
    private final ScoringService scoringService;

    public IngestionService(ItemRepository itemRepository, ScoringService scoringService) {
        this.itemRepository = itemRepository;
        this.scoringService = scoringService;
    }

    /** @return true if the item was new and stored, false if it was a duplicate. */
    public boolean ingest(RawItem raw) {
        if (itemRepository.existsBySourceAndExternalId(raw.source(), raw.externalId())) {
            return false;
        }

        Item item = new Item(raw.source(), raw.externalId(), raw.title());
        item.setUrl(raw.url());
        item.setRawText(raw.description());
        item.setInKev(raw.inKev());
        item.setCvssScore(raw.cvssScore());
        item.setPublishedAt(raw.publishedAt());
        item.setCollectedAt(Instant.now());
        item.setTags(raw.tags() != null ? new ArrayList<>(raw.tags()) : new ArrayList<>());

        // Summary stays null until the LLM step (SummaryService) is wired in.
        scoringService.score(item);

        itemRepository.save(item);
        return true;
    }

    public int ingestAll(Iterable<RawItem> rawItems) {
        int stored = 0;
        for (RawItem raw : rawItems) {
            try {
                if (ingest(raw)) {
                    stored++;
                }
            } catch (Exception e) {
                log.warn("Failed to ingest item {} from {}: {}",
                        raw.externalId(), raw.source(), e.getMessage());
            }
        }
        return stored;
    }
}
