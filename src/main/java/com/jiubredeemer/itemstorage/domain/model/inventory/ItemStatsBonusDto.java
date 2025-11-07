package com.jiubredeemer.itemstorage.domain.model.inventory;

import lombok.Data;

import java.util.List;

@Data
public class ItemStatsBonusDto {
    private List<InventoryItemDto> items;
    private Integer totalBonus;
}
