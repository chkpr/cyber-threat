package com.chkpr.cyberthreat.item;

import com.chkpr.cyberthreat.item.DigestDtos.ActionRequest;
import com.chkpr.cyberthreat.item.DigestDtos.DigestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/digest")
    public DigestResponse digest() {
        return itemService.getDigest();
    }

    @PostMapping("/items/{id}/action")
    public ResponseEntity<Void> action(@PathVariable Long id, @RequestBody ActionRequest request) {
        itemService.applyAction(id, request.action());
        return ResponseEntity.noContent().build();
    }
}
