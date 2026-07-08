package com.chkpr.cyberthreat.process;

import com.chkpr.cyberthreat.item.Criticality;
import com.chkpr.cyberthreat.item.Item;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Assigns a score and criticality to an item.
 *
 * The critical-routing rule is the important part: anything in the CISA KEV
 * catalog, or with a CVSS >= 9, is flagged ALERT and surfaces at the top of the
 * digest regardless of its numeric score.
 *
 * Source weights live here for now (in-memory). When we wire the learning loop,
 * they move to a Source entity and get adjusted from user actions.
 */
@Service
public class ScoringService {

    private static final double CVSS_ALERT_THRESHOLD = 9.0;

    private static final Map<String, Double> SOURCE_WEIGHTS = Map.of(
            "CISA KEV", 5.0,
            "Hacker News", 2.0
    );

    public void score(Item item) {
        boolean alert = item.isInKev()
                || (item.getCvssScore() != null && item.getCvssScore() >= CVSS_ALERT_THRESHOLD);
        item.setCriticality(alert ? Criticality.ALERT : Criticality.NORMAL);

        double score = SOURCE_WEIGHTS.getOrDefault(item.getSource(), 1.0);
        score += recencyBonus(item.getPublishedAt());
        if (item.getCvssScore() != null) {
            score += item.getCvssScore() / 2.0;
        }
        if (alert) {
            score += 10.0;
        }
        item.setScore(score);
    }

    private double recencyBonus(Instant publishedAt) {
        if (publishedAt == null) {
            return 0.0;
        }
        long hours = Duration.between(publishedAt, Instant.now()).toHours();
        if (hours < 0) {
            return 3.0;
        }
        if (hours <= 24) {
            return 3.0;
        }
        if (hours <= 72) {
            return 1.5;
        }
        if (hours <= 168) {
            return 0.5;
        }
        return 0.0;
    }
}
