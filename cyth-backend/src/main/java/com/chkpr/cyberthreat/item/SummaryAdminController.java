package com.chkpr.cyberthreat.item;
 
import com.chkpr.cyberthreat.process.SummaryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.Map;
 
/**
 * Manual trigger for summarization, handy for testing without waiting for the
 * scheduled run. Local/dev only — no authentication.
 */
@RestController
@RequestMapping("/api/admin")
public class SummaryAdminController {
 
    private final SummaryService summaryService;
 
    public SummaryAdminController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }
 
    @PostMapping("/summarize")
    public Map<String, Object> summarize(@RequestParam(defaultValue = "10") int batch) {
        int done = summaryService.enrichPending(batch);
        return Map.of("summarized", done);
    }
}