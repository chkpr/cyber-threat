package com.chkpr.cyberthreat.item;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"})
)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier native to the source (CVE id, HN story id...). Used for dedup. */
    @Column(name = "external_id", nullable = false)
    private String externalId;

    /** Source name, e.g. "CISA KEV" or "Hacker News". */
    @Column(nullable = false)
    private String source;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(length = 2000)
    private String url;

    /** Raw text used later by the LLM to produce the summary. */
    @Column(length = 4000)
    private String rawText;

    /** Two-line summary produced by the LLM (null until summarized). */
    @Column(length = 1000)
    private String summary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_tag", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    private double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Criticality criticality = Criticality.NORMAL;

    /** CVSS base score when the item is a vulnerability, null otherwise. */
    private Double cvssScore;

    /** True when the CVE appears in the CISA KEV catalog. */
    private boolean inKev;

    private Instant publishedAt;

    @Column(nullable = false)
    private Instant collectedAt;

    /** null until the user acts on the item; feeds the scoring loop. */
    @Enumerated(EnumType.STRING)
    private ItemAction userAction;

    protected Item() {
    }

    public Item(String source, String externalId, String title) {
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.collectedAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public Criticality getCriticality() { return criticality; }
    public void setCriticality(Criticality criticality) { this.criticality = criticality; }

    public Double getCvssScore() { return cvssScore; }
    public void setCvssScore(Double cvssScore) { this.cvssScore = cvssScore; }

    public boolean isInKev() { return inKev; }
    public void setInKev(boolean inKev) { this.inKev = inKev; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Instant collectedAt) { this.collectedAt = collectedAt; }

    public ItemAction getUserAction() { return userAction; }
    public void setUserAction(ItemAction userAction) { this.userAction = userAction; }
}
