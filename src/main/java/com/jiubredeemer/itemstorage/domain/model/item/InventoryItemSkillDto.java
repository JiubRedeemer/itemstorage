package com.jiubredeemer.itemstorage.domain.model.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryItemSkillDto {
    private UUID id;
    private UUID inventoryItemId;
    private UUID itemSkillId;
    private ItemSkillDto skill;
    private Integer currentCharges;
}
