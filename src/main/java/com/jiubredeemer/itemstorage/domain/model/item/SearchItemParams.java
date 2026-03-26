package com.jiubredeemer.itemstorage.domain.model.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemParams {
    private String searchQuery;
    private Integer limit;
    private Timestamp lastSeenCreatedAt;
    private UUID lastSeenId;
}
