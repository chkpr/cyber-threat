package com.chkpr.cyberthreat.item;

import java.time.Instant;
import java.util.List;

/**
 * Response shapes for the digest endpoint. Kept as records, separate from the
 * JPA entity, so the API contract and the persistence model can evolve apart.
 */
public final class DigestDtos {

    private DigestDtos() {
    }

    public record DigestStats(
            long newToday,
            long criticalAlerts,
            long sources,
            long toRead
    ) {
    }

    public record ItemDto(
            Long id,
            String source,
            String title,
            String url,
            String summary,
            List<String> tags,
            double score,
            Criticality criticality,
            Double cvssScore,
            boolean inKev,
            Instant publishedAt
    ) {
        public static ItemDto from(Item item) {
            return new ItemDto(
                    item.getId(),
                    item.getSource(),
                    item.getTitle(),
                    item.getUrl(),
                    item.getSummary(),
                    item.getTags(),
                    item.getScore(),
                    item.getCriticality(),
                    item.getCvssScore(),
                    item.isInKev(),
                    item.getPublishedAt()
            );
        }
    }

    public record DigestResponse(
            DigestStats stats,
            List<ItemDto> alerts,
            List<ItemDto> items
    ) {
    }

    public record ActionRequest(ItemAction action) {
    }
}
