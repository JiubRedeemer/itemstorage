package com.jiubredeemer.itemstorage.domain.model.money;

import lombok.Data;

import java.util.UUID;

@Data
public class MoneyDto {
    private UUID id;
    private UUID inventoryId;
    private Long goldenCount;
    private Long silverCount;
    private Long copperCount;
}
