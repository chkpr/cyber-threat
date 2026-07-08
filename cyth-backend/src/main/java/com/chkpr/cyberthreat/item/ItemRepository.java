package com.chkpr.cyberthreat.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    /** Items not yet acted on: the pool the digest is built from. */
    List<Item> findByUserActionIsNull();

    long countByUserActionIsNullAndCriticality(Criticality criticality);

    long countByCollectedAtAfter(Instant instant);

    long countByUserAction(ItemAction action);
}
