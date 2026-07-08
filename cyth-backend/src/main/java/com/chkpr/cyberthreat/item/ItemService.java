package com.chkpr.cyberthreat.item;

import com.chkpr.cyberthreat.item.DigestDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public DigestResponse getDigest() {
        List<Item> pool = itemRepository.findByUserActionIsNull();

        List<ItemDto> alerts = pool.stream()
                .filter(i -> i.getCriticality() == Criticality.ALERT)
                .sorted(Comparator.comparingDouble(Item::getScore).reversed())
                .map(ItemDto::from)
                .toList();

        List<ItemDto> items = pool.stream()
                .filter(i -> i.getCriticality() == Criticality.NORMAL)
                .sorted(Comparator.comparingDouble(Item::getScore).reversed())
                .map(ItemDto::from)
                .toList();

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long sources = pool.stream().map(Item::getSource).distinct().count();

        DigestStats stats = new DigestStats(
                itemRepository.countByCollectedAtAfter(startOfDay),
                itemRepository.countByUserActionIsNullAndCriticality(Criticality.ALERT),
                sources,
                itemRepository.countByUserAction(ItemAction.READ_LATER)
        );

        return new DigestResponse(stats, alerts, items);
    }

    @Transactional
    public void applyAction(Long id, ItemAction action) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id));
        item.setUserAction(action);
        itemRepository.save(item);
        // Learning loop hook: adjust source/tag weights from this action later.
    }
}
