package com.chkpr.cyberthreat.collect;

import java.time.Instant;
import java.util.List;

/**
 * Normalized item coming out of a collector, before dedup / scoring / persistence.
 */
public record RawItem(
        String source,
        String externalId,
        String title,
        String url,
        String description,
        Instant publishedAt,
        boolean inKev,
        Double cvssScore,
        List<String> tags
) {
}
