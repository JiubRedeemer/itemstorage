package com.jiubredeemer.itemstorage.domain.model.shop;

import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopItemDto {
    private UUID id;
    private UUID shopId;
    private UUID itemId;
    private Long priceGold;
    private Long priceSilver;
    private Long priceCopper;
    private Integer sortOrder;
    /**
     * Разрешённый предмет каталога/комнаты (заполняется при листинге витрины).
     */
    private ItemDto item;
}
