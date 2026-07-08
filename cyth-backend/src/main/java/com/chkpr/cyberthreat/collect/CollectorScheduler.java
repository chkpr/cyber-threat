package com.chkpr.cyberthreat.collect;

import com.chkpr.cyberthreat.process.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orchestrates every Collector on the configured cron. Spring injects the full
 * list of Collector beans, so adding a source requires no change here.
 */
@Component
public class CollectorScheduler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectorScheduler.class);

    private final List<Collector> collectors;
    private final IngestionService ingestionService;

    @Value("${app.collect.run-on-startup:true}")
    private boolean runOnStartup;

    public CollectorScheduler(List<Collector> collectors, IngestionService ingestionService) {
        this.collectors = collectors;
        this.ingestionService = ingestionService;
    }

    @Scheduled(cron = "${app.collect.cron:0 0 * * * *}")
    public void runAll() {
        log.info("Collection run started ({} collectors)", collectors.size());
        int total = 0;
        for (Collector collector : collectors) {
            try {
                List<RawItem> items = collector.collect();
                int stored = ingestionService.ingestAll(items);
                total += stored;
                log.info("{}: {} new items stored", collector.name(), stored);
            } catch (Exception e) {
                log.error("Collector {} failed: {}", collector.name(), e.getMessage());
            }
        }
        log.info("Collection run finished: {} new items total", total);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (runOnStartup) {
            runAll();
        }
    }
}
