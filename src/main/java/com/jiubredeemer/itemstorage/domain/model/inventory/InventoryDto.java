package com.jiubredeemer.itemstorage.domain.model.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDto {
    private UUID id;
    private UUID roomId;
    private UUID characterId;
    private Long totalWeight;
    private List<InventoryItemDto> items;
}
