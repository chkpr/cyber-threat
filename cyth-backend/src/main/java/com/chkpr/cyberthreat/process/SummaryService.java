package com.chkpr.cyberthreat.process;

import com.chkpr.cyberthreat.item.Item;
import com.chkpr.cyberthreat.item.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fills in the two-line summary of items using a local LLM (Ollama) through
 * Spring AI. To avoid hammering the model on the first big collection run,
 * summarization is decoupled from ingestion: a scheduled task summarizes only
 * the most relevant items still missing a summary, a small batch at a time.
 */
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private static final String SYSTEM_PROMPT = """
            Tu es un analyste en cybersécurité. On te fournit le titre (et parfois une
            description) d'un élément de veille. Résume-le en français, en deux phrases
            maximum, de façon factuelle et concise. Va droit au but : pas d'introduction,
            pas de formule de politesse, pas de mise en forme.
            """;

    private final ChatClient chatClient;
    private final ItemRepository itemRepository;

    @Value("${app.summary.enabled:true}")
    private boolean enabled;

    @Value("${app.summary.batch-size:10}")
    private int batchSize;

    public SummaryService(ChatClient.Builder chatClientBuilder, ItemRepository itemRepository) {
        this.chatClient = chatClientBuilder.build();
        this.itemRepository = itemRepository;
    }

    /** Periodically summarizes the highest-scored items that have no summary yet. */
    @Scheduled(
            fixedDelayString = "${app.summary.interval-ms:120000}",
            initialDelayString = "${app.summary.initial-delay-ms:20000}"
    )
    public void enrichScheduled() {
        if (enabled) {
            enrichPending(batchSize);
        }
    }

    /** Summarizes up to {@code limit} pending items. Returns how many were done. */
    public int enrichPending(int limit) {
        List<Item> pending = itemRepository.findBySummaryIsNullOrderByScoreDesc(PageRequest.of(0, limit));
        int done = 0;
        for (Item item : pending) {
            try {
                String summary = summarize(item);
                if (summary != null && !summary.isBlank()) {
                    item.setSummary(truncate(summary.trim(), 1000));
                    itemRepository.save(item);
                    done++;
                }
            } catch (Exception e) {
                log.warn("Summary failed for item {} ({}): {}",
                        item.getId(), item.getExternalId(), e.getMessage());
            }
        }
        if (done > 0) {
            log.info("Summarized {} items", done);
        }
        return done;
    }

    public String summarize(Item item) {
        StringBuilder input = new StringBuilder(item.getTitle());
        if (item.getRawText() != null && !item.getRawText().isBlank()) {
            input.append("\n\n").append(item.getRawText());
        }
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(input.toString())
                .call()
                .content();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}