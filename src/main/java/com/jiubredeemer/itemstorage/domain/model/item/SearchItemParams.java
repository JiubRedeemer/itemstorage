package com.jiubredeemer.itemstorage.domain.model.item;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SearchItemParams {
    private final String searchQuery;
    private final Integer limit;
    private final LocalDateTime lastSeenCreatedAt;
    private final UUID lastSeenId;
}
