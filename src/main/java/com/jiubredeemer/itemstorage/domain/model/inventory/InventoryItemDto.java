package com.jiubredeemer.itemstorage.domain.model.inventory;

import com.jiubredeemer.itemstorage.domain.model.item.InventoryItemSkillDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryItemDto {
    private UUID id;
    private UUID inventoryId;
    private UUID itemId;
    private ItemDto item;
    private Long count;
    private Boolean inUse;
    private Boolean requirementsOk;
    private List<InventoryItemSkillDto> skills;
}
